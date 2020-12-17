package util

// TODO remove exponential complexity in CompoundPrice operations
// TODO use a real NonEmptyList
object price {

  import Price.SinglePrice

  sealed trait Price extends Product with Serializable {
    def +(that: Price): Price
    def *(factor: BigDecimal): Price
    def to(currency: Currency): Rates => Option[SinglePrice]
    def -(that: Price): Price               = this + that.changeSign
    def inARS: Rates => Option[SinglePrice] = to(Currency.ARS)
    def inEUR: Rates => Option[SinglePrice] = to(Currency.EUR)
    protected def changeSign: Price
  }

  object Price {

    def fromStringUnsafe(str: String): Price =
      str.split('+').map(singlePriceFromString).reduce[Price](_ + _)

    private def singlePriceFromString(input: String) =
      input
        .replace('(', ' ')
        .replace(')', ' ')
        .strip()
        .split(' ')
        .map(_.strip())
        .toList match {
        case currency :: amount :: Nil => SinglePrice(BigDecimal(amount), Currency fromStringUnsafe currency)
      }

    case class SinglePrice(
      amount: Amount,
      currency: Currency,
    ) extends Price
        with Ordered[SinglePrice] {

      override def compare(that: SinglePrice): Int = this.amount compare that.amount

      override def +(that: Price): Price =
        that match {
          case SinglePrice(amount, currency) if this.currency == currency => copy(this.amount + amount)
          case that: SinglePrice                                          => CompoundPrice(::(this, that :: Nil))
          case _                                                          => that + this
        }

      override def *(factor: BigDecimal): Price = copy(this.amount * factor)

      override def to(currency: Currency): Rates => Option[SinglePrice] =
        rates => rates.get(this.currency -> currency).map(rate => SinglePrice(amount * rate, currency))

      override protected def changeSign: Price                          = copy(-this.amount)

      override def toString: String = s"($currency $amount)"

    }

    case class CompoundPrice(
      prices: NonEmptyList[SinglePrice]
    ) extends Price {

      override def +(that: Price): Price =
        that match {
          case that: SinglePrice     => CompoundPrice(::(that, prices))
          case CompoundPrice(prices) => CompoundPrice(::(this.prices.head, this.prices.tail concat prices))
        }

      override def *(factor: BigDecimal): Price = copy(prices.map(_ * factor).asInstanceOf[::[SinglePrice]])

      override def to(currency: Currency): Rates => Option[SinglePrice] = {
        import util.std.list.syntax.ListOps
        rates =>
          prices
            .map(_.to(currency)(rates))
            .sequence
            .map(_.map(_.amount).sum)
            .map(SinglePrice(_, currency))
      }

      override protected def changeSign: Price = copy(prices.map(_ * -1).asInstanceOf[::[SinglePrice]])

      override def equals(that: Any): Boolean =
        that match {
          case CompoundPrice(thatPrices) =>
            this.prices.groupBy(_.currency).view.mapValues(_.map(_.amount).sum).toList ==
              thatPrices.groupBy(_.currency).view.mapValues(_.map(_.amount).sum).toList
          case _                         => false
        }

      override def toString: String =
        this.prices
          .groupBy(_.currency)
          .view
          .mapValues(_.map(_.amount).sum)
          .toList
          .sortBy(_._1)
          .map { case (currency, amount) => s"($currency $amount)" }
          .mkString(" + ")

    }

  }

  sealed trait Sign extends Product with Serializable {
    def change: Sign
    def *(number: BigDecimal): BigDecimal
  }
  object Sign {
    case object Plus  extends Sign {
      override def change: Sign                      = Minus
      override def *(number: BigDecimal): BigDecimal = number
    }
    case object Minus extends Sign {
      override def change: Sign                      = Plus
      override def *(number: BigDecimal): BigDecimal = -number
    }
  }

  sealed trait Currency extends Product with Serializable with Ordered[Currency] {
    def *(amount: Amount): SinglePrice        = SinglePrice(amount, this)
    override def compare(that: Currency): Int = this.toString compare that.toString
  }

  object Currency {

    case object ARS extends Currency
    case object EUR extends Currency

    def fromStringUnsafe(currency: String): Currency =
      currency match {
        case "ARS" => ARS
        case "EUR" => EUR
      }

    val all: List[Currency] = ARS :: EUR :: Nil

  }

  type Amount = BigDecimal
  type Rates  = Map[(Currency, Currency), BigDecimal]

  object Rates {
    val identity: Rates =
      (for { from <- Currency.all; to <- Currency.all } yield from -> to).map(_ -> (BigDecimal exact 1)).toMap
  }

  type NonEmptyList[T] = ::[T]

}
