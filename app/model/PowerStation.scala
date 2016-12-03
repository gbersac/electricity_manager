package model

case class PowerStation(
  id: Int,
  typePW: String,
  code: String,
  maxCapacity: Int,
  proprietary: User
)
