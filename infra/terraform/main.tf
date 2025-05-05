// main.tf
terraform {
  backend "s3" {
    bucket       = "solanceworkflowemanners"
    key          = "solance-workflow/infra.tfstate"
    region       = "eu-west-1"
    encrypt      = true
    use_lockfile = true
  }
  required_version = ">= 1.0.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}


provider "aws" {
  region = var.aws_region
}

// VPC Module: public and private subnets with NAT
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "3.18.0"

  name                 = "solance-vpc"
  cidr                 = "10.0.0.0/16"
  azs                  = ["eu-west-1a", "eu-west-1b"]
  public_subnets       = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnets      = ["10.0.3.0/24", "10.0.4.0/24"]
  enable_nat_gateway   = true
  single_nat_gateway   = true
}

# data "aws_availability_zones" "available" {}  <-- removed due to IAM limits

// ECS Cluster
resource "aws_ecs_cluster" "solance" {
  name = var.cluster_name
}

// IAM Role: ECS Task Execution
resource "aws_iam_role" "ecs_task_execution_role" {
  name               = "${var.cluster_name}-ecs-exec-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_execution_assume.json
}

data "aws_iam_policy_document" "ecs_task_execution_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role_policy_attachment" "ecs_exec_attach" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

// IAM Role: ECS Task Role (app permissions)
resource "aws_iam_role" "ecs_task_role" {
  name               = "${var.cluster_name}-ecs-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_task_assume.json
}

data "aws_iam_policy_document" "ecs_task_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_policy" "ecs_task_policy" {
  name        = "${var.cluster_name}-ecs-task-policy"
  description = "Allow DynamoDB & EventBridge access"
  policy      = data.aws_iam_policy_document.ecs_task_policy.json
}

data "aws_iam_policy_document" "ecs_task_policy" {
  statement {
    actions = [
      "dynamodb:PutItem",
      "dynamodb:UpdateItem",
      "dynamodb:GetItem",
      "events:PutEvents"
    ]
    resources = [
      aws_dynamodb_table.workflow.arn,
      aws_cloudwatch_event_bus.workflow.arn
    ]
  }
}

resource "aws_iam_role_policy_attachment" "ecs_task_policy_attach" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = aws_iam_policy.ecs_task_policy.arn
}

// CloudWatch Log Group for ECS
resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/${var.cluster_name}"
  retention_in_days = 30
}

// Security Groups
resource "aws_security_group" "lb" {
  name        = "${var.cluster_name}-lb-sg"
  description = "Allow HTTP inbound"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "ecs" {
  name        = "${var.cluster_name}-ecs-sg"
  description = "Allow inbound from ALB and VPC"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.lb.id]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }
}

// Application Load Balancer
resource "aws_lb" "api" {
  name               = "${var.cluster_name}-alb"
  load_balancer_type = "application"
  subnets            = module.vpc.public_subnets
  security_groups    = [aws_security_group.lb.id]
}

resource "aws_lb_target_group" "api" {
  name_prefix = "api-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"

  health_check {
    path                = "/health"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 2
  }
  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.api.arn
  port              = "80"
  protocol          = "HTTP"
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn
  }
}

// ECS Task Definition: API
resource "aws_ecs_task_definition" "api" {
  family                   = "${var.cluster_name}-api"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "api"
      image     = var.api_image
      portMappings = [{ containerPort = 8080, protocol = "tcp" }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "api"
        }
      }
      environment = [
        { name = "DDB_TABLE", value = aws_dynamodb_table.workflow.name },
        { name = "EVENT_BUS", value = aws_cloudwatch_event_bus.workflow.name },
        { "name": "AWS_REGION", "value": var.aws_region }
      ]
    }
  ])
}

// ECS Service: API
resource "aws_ecs_service" "api" {
  name            = "${var.cluster_name}-api"
  cluster         = aws_ecs_cluster.solance.id
  task_definition = aws_ecs_task_definition.api.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = module.vpc.private_subnets
    security_groups = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "api"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.http]
}

// Optional: ECS Task & Service for Worker (long-running)
resource "aws_ecs_task_definition" "worker" {
  family                   = "${var.cluster_name}-worker"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name      = "worker"
      image     = var.worker_image
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.ecs.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "worker"
        }
      }
      environment = [
        { name = "DDB_TABLE", value = aws_dynamodb_table.workflow.name },
        { name = "EVENT_BUS", value = aws_cloudwatch_event_bus.workflow.name },
        { name: "AWS_REGION", "value": var.aws_region },
        { name  = "SQS_QUEUE", value = aws_sqs_queue.workflow.name}
      ]
    }
  ])
}

resource "aws_ecs_service" "worker" {
  name            = "${var.cluster_name}-worker"
  cluster         = aws_ecs_cluster.solance.id
  task_definition = aws_ecs_task_definition.worker.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = module.vpc.private_subnets
    security_groups = [aws_security_group.ecs.id]
    assign_public_ip = false
  }
}

// DynamoDB Table for Workflow
resource "aws_dynamodb_table" "workflow" {
  name         = "solance-workflow"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "eventId"

  attribute {
    name = "eventId"
    type = "S"
  }
}

# 1. Create the SQS queue
resource "aws_sqs_queue" "workflow" {
  name                       = "solance-workflow-queue"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 1209600  # 14 days
}

# 2. Allow EventBridge to send to it
resource "aws_sqs_queue_policy" "workflow_from_eb" {
  queue_url = aws_sqs_queue.workflow.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "events.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.workflow.arn
      Condition = {
        ArnEquals = {
          "aws:SourceArn" = aws_cloudwatch_event_bus.workflow.arn
        }
      }
    }]
  })
}

# 3. Create an EventBridge rule for your four event types
resource "aws_cloudwatch_event_rule" "workflow" {
  name        = "workflow-rule"
  event_bus_name = aws_cloudwatch_event_bus.workflow.name

  event_pattern = jsonencode({
    "detail-type": [
      "RegisterCustomer",
      "OpenAccount",
      "Deposit",
      "Payout"
    ]
  })
}

# 4. Point that rule at your SQS queue
resource "aws_cloudwatch_event_target" "to_sqs" {
  rule      = aws_cloudwatch_event_rule.workflow.name
  arn       = aws_sqs_queue.workflow.arn
  event_bus_name = aws_cloudwatch_event_bus.workflow.name
}

# 5. Give EventBridge permission to invoke that target
resource "aws_cloudwatch_event_permission" "allow_sqs" {
  principal    = "*"
  statement_id = "AllowEventBridgeToSendToSQS"
  action       = "events:PutEvents"
  event_bus_name = aws_cloudwatch_event_bus.workflow.name
}


// EventBridge Bus
resource "aws_cloudwatch_event_bus" "workflow" {
  name = "workflow-bus"
}

output "alb_dns" {
  description = "API Load Balancer DNS"
  value       = aws_lb.api.dns_name
}

output "dynamodb_table" {
  description = "DynamoDB workflow table name"
  value       = aws_dynamodb_table.workflow.name
}

output "event_bus" {
  description = "EventBridge bus name"
  value       = aws_cloudwatch_event_bus.workflow.name
}
