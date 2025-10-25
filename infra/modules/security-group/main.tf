resource "aws_security_group" "main" {
  name        = "shortlink-${var.environment}-${var.name}-sg"
  description = var.description
  vpc_id      = var.vpc_id

  # 분리형 룰 리소스만 사용 (충돌 방지)
  ingress = []
  egress  = []

  tags = {
    Name        = "shortlink-${var.environment}-${var.name}-sg"
    Environment = var.environment
  }
}

# Ingress Rules (분리형)
resource "aws_vpc_security_group_ingress_rule" "this" {
  for_each = { for idx, rule in var.ingress_rules : idx => rule }

  security_group_id = aws_security_group.main.id

  # 프로토콜/포트: -1이면 포트는 null
  ip_protocol = try(each.value.protocol, null) == "-1" ? "-1" : each.value.protocol
  from_port   = try(each.value.protocol, null) == "-1" ? null : try(each.value.from_port, null)
  to_port     = try(each.value.protocol, null) == "-1" ? null : try(each.value.to_port, null)

  # CIDR vs SG-ID 중 하나만 (동시 지정 금지)
  cidr_ipv4                    = try(each.value.cidr_blocks, null)
  referenced_security_group_id = try(each.value.source_security_group_id, null)

  description = try(each.value.description, "")

  tags = {
    Name = "shortlink-${var.environment}-${var.name}-ingress-${each.key}"
  }
}

# Egress Rules (분리형)
resource "aws_vpc_security_group_egress_rule" "this" {
  for_each = { for idx, rule in var.egress_rules : idx => rule }

  security_group_id = aws_security_group.main.id

  # 프로토콜/포트: -1이면 포트는 null
  ip_protocol = try(each.value.protocol, null) == "-1" ? "-1" : each.value.protocol
  from_port   = try(each.value.protocol, null) == "-1" ? null : try(each.value.from_port, null)
  to_port     = try(each.value.protocol, null) == "-1" ? null : try(each.value.to_port, null)

  cidr_ipv4   = try(each.value.cidr_blocks, null)
  description = try(each.value.description, "")

  tags = {
    Name = "shortlink-${var.environment}-${var.name}-egress-${each.key}"
  }
}
