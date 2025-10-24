resource "aws_security_group" "main" {
  name        = "shortlink-${var.environment}-${var.name}-sg"
  description = var.description
  vpc_id      = var.vpc_id

  tags = {
    Name        = "shortlink-${var.environment}-${var.name}-sg"
    Environment = var.environment
  }
}

# Ingress Rules
resource "aws_vpc_security_group_ingress_rule" "this" {
  for_each = { for idx, rule in var.ingress_rules : idx => rule }

  security_group_id = aws_security_group.main.id

  ip_protocol = each.value.protocol
  from_port   = each.value.from_port
  to_port     = each.value.to_port

  cidr_ipv4                    = lookup(each.value, "cidr_blocks", null)
  referenced_security_group_id = lookup(each.value, "source_security_group_id", null)

  description = lookup(each.value, "description", "")

  tags = {
    Name = "shortlink-${var.environment}-${var.name}-ingress-${each.key}"
  }
}

# Egress Rules
resource "aws_vpc_security_group_egress_rule" "this" {
  for_each = { for idx, rule in var.egress_rules : idx => rule }

  security_group_id = aws_security_group.main.id

  ip_protocol = each.value.protocol
  from_port   = each.value.from_port
  to_port     = each.value.to_port

  cidr_ipv4 = lookup(each.value, "cidr_blocks", null)

  description = lookup(each.value, "description", "")

  tags = {
    Name = "shortlink-${var.environment}-${var.name}-egress-${each.key}"
  }
}
