variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
}

variable "name" {
  description = "Name of the security group"
  type        = string
}

variable "description" {
  description = "Description of the security group"
  type        = string
  default     = "Managed by Terraform"
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "ingress_rules" {
  description = "List of ingress rules"
  type = list(object({
    from_port                = number
    to_port                  = number
    protocol                 = string
    cidr_blocks              = optional(string)
    source_security_group_id = optional(string)
    description              = optional(string)
  }))
  default = []

  validation {
    condition = alltrue([
      for rule in var.ingress_rules :
      (
        rule.cidr_blocks != null && rule.cidr_blocks != ""
      ) || (
        rule.source_security_group_id != null && rule.source_security_group_id != ""
      )
    ])
    error_message = "Each ingress rule must provide either a non-empty cidr_blocks or a source_security_group_id."
  }
}

variable "egress_rules" {
  description = "List of egress rules"
  type = list(object({
    from_port   = number
    to_port     = number
    protocol    = string
    cidr_blocks = optional(string)
    description = optional(string)
  }))
  default = [{
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = "0.0.0.0/0"
    description = "Allow all outbound traffic"
  }]
}
