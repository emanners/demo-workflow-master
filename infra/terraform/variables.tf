// variables.tf

variable "aws_region" {
  description = "AWS region to deploy resources in"
  type        = string
  default     = "eu-west-1"
}

variable "cluster_name" {
  description = "Name of the ECS cluster and related resources"
  type        = string
  default     = "solance-cluster"
}

variable "api_image" {
  description = "Docker image URI for the API service"
  type        = string
  default     = "472842289688.dkr.ecr.eu-west-1.amazonaws.com/solance-api:latest"
}

variable "worker_image" {
  description = "Docker image URI for the Worker service"
  type        = string
  default     = "472842289688.dkr.ecr.eu-west-1.amazonaws.com/solance-worker:latest"
}
