// outputs.tf

output "vpc_id" {
  description = "ID of the VPC created"
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "IDs of the public subnets"
  value       = module.vpc.public_subnets
}

output "private_subnet_ids" {
  description = "IDs of the private subnets"
  value       = module.vpc.private_subnets
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.solance.name
}

output "api_load_balancer_dns" {
  description = "DNS name of the API Application Load Balancer"
  value       = aws_lb.api.dns_name
}

output "dynamodb_table_name" {
  description = "Name of the workflow DynamoDB table"
  value       = aws_dynamodb_table.workflow.name
}

output "event_bus_name" {
  description = "Name of the EventBridge bus"
  value       = aws_cloudwatch_event_bus.workflow.name
}

output "ecs_task_execution_role_arn" {
  description = "ARN of the ECS task execution IAM role"
  value       = aws_iam_role.ecs_task_execution_role.arn
}

output "ecs_task_role_arn" {
  description = "ARN of the ECS task IAM role"
  value       = aws_iam_role.ecs_task_role.arn
}
