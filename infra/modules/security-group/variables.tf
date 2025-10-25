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

# ─────────────────────────────────────────────
# Ingress rules
# - protocol = "-1" (all) 이면 from/to 포트는 지정하지 않아야 함
# - CIDR 또는 SG-ID 중 하나는 반드시 제공
# ─────────────────────────────────────────────
variable "ingress_rules" {
  description = "List of ingress rules"
  type = list(object({
    protocol                 = string
    from_port                = optional(number)
    to_port                  = optional(number)
    cidr_blocks              = optional(string)
    source_security_group_id = optional(string)
    description              = optional(string)
  }))
  default = []

  # CIDR 또는 SG-ID 제공
  validation {
    condition = alltrue([
      for rule in var.ingress_rules :
      (
        try(rule.cidr_blocks, "") != ""
      ) || (
        try(rule.source_security_group_id, "") != ""
      )
    ])
    error_message = "Each ingress rule must provide either a non-empty cidr_blocks or a source_security_group_id."
  }

  # ✅ 수정된 포트 검증
  validation {
    condition = alltrue([
      for rule in var.ingress_rules :
      (
        rule.protocol == "-1"
        && rule.from_port == null
        && rule.to_port   == null
      )
      ||
      (
        rule.protocol != "-1"
        && rule.from_port != null
        && rule.to_port   != null
      )
    ])
    error_message = "Ingress rule: when protocol is \"-1\", from_port/to_port must be omitted; otherwise both ports must be set."
  }
}

# ─────────────────────────────────────────────
# Egress rules
# - protocol = "-1" (all) 이면 from/to 포트는 지정하지 않아야 함
# ─────────────────────────────────────────────
# Egress rules
variable "egress_rules" {
  description = "List of egress rules"
  type = list(object({
    protocol    = string
    from_port   = optional(number)
    to_port     = optional(number)
    cidr_blocks = optional(string)
    description = optional(string)
  }))

  # 모든 아웃바운드 허용 (포트 지정하지 않음!)
  default = [
    {
      protocol    = "-1"
      cidr_blocks = "0.0.0.0/0"
      description = "Allow all outbound traffic"
    }
  ]

  # ✅ 수정된 검증 로직
  validation {
    condition = alltrue([
      for rule in var.egress_rules :
      (
        rule.protocol == "-1"
        && rule.from_port == null
        && rule.to_port   == null
      )
      ||
      (
        rule.protocol != "-1"
        && rule.from_port != null
        && rule.to_port   != null
      )
    ])
    error_message = "Egress rule: when protocol is \"-1\", from_port/to_port must be omitted; otherwise both ports must be set."
  }
}
