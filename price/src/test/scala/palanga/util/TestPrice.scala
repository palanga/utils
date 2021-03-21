package palanga.util

import palanga.util.price.Currency._
import palanga.util.price._
import zio.test.Assertion.{ equalTo, isLessThan, isSome }
import zio.test._

object TestPrice extends DefaultRunnableSpec {

  private val EUR_ARS_RATE = 200

  private val rates: Rates = Map(
    (EUR -> ARS) -> EUR_ARS_RATE,
    (ARS -> ARS) -> 1,
    (EUR -> EUR) -> 1,
  )

  private val singlePriceSuite =
    suite("single")(
      test("summing") {
        val actualPrice   = ARS * 100 + ARS * 20
        val expectedPrice = ARS * 120
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("subtracting") {
        val actualPrice   = ARS * 100 - ARS * 120
        val expectedPrice = ARS * -20
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("multiplying") {
        val actualPrice   = (ARS * 100) * 0.3
        val expectedPrice = ARS * 30
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("multiplying negative") {
        val actualPrice   = (ARS * 100) * -0.3
        val expectedPrice = ARS * -30
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("ordering") {
        val min = ARS * 100
        val max = ARS * 200
        assert(min)(isLessThan(max))
      },
      test("ordering negative") {
        val max = ARS * -100
        val min = ARS * -200
        assert(min)(isLessThan(max))
      },
    )

  private val compoundPriceSuite =
    suite("compound")(
      test("converting") {
        val eur = EUR * 10
        val ars = ARS * 10 * EUR_ARS_RATE
        assert(eur inARS rates)(isSome(equalTo(ars)))
      },
      test("converting complex") {
        val eur = ARS * 100 + EUR * 10
        val ars = ARS * 100 + ARS * 10 * EUR_ARS_RATE
        assert(eur inARS rates)(isSome(equalTo(ars)))
      },
      test("summing") {
        val actualPrice   = ARS * 100 + EUR * 10 + ARS * 20 + EUR * 90
        val expectedPrice = ARS * 120 + EUR * 100
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("subtracting") {
        val actualPrice   = ARS * 100 - EUR * 10 + ARS * 10
        val expectedPrice = ARS * 110 - EUR * 10
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("multiplying") {
        val actualPrice   = (ARS * 100 + EUR * 10) * 2
        val expectedPrice = ARS * 200 + EUR * 20
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("complex operation") {
        val actualPrice   = (ARS * 100 - EUR * 10 + ARS * 200) * 2 + ARS * 1
        val expectedPrice = ARS * 601 - EUR * 20
        assert(actualPrice)(equalTo(expectedPrice))
      },
    )

  private val zeroPriceSuite =
    suite("zero")(
      test("summing to zero") {
        val actualPrice   = Price.Zero + Price.Zero
        val expectedPrice = Price.Zero
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("summing to single") {
        val actualPrice   = ARS * 100 + Price.Zero
        val expectedPrice = ARS * 100
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("summing to compound") {
        val actualPrice   = ARS * 100 + EUR * 20 + Price.Zero
        val expectedPrice = ARS * 100 + EUR * 20
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("subtracting to zero") {
        val actualPrice   = Price.Zero - Price.Zero
        val expectedPrice = Price.Zero
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("subtracting to single") {
        val actualPrice   = ARS * 100 - Price.Zero
        val expectedPrice = ARS * 100
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("subtracting to compound") {
        val actualPrice   = ARS * 100 + EUR * 20 - Price.Zero
        val expectedPrice = ARS * 100 + EUR * 20
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("multiplying") {
        val actualPrice   = Price.Zero * 2
        val expectedPrice = Price.Zero
        assert(actualPrice)(equalTo(expectedPrice))
      },
      test("complex operation") {
        val actualPrice   = (Price.Zero + ARS * 100 - Price.Zero - EUR * 10 + ARS * 200 + Price.Zero) * 2 + ARS * 1
        val expectedPrice = ARS * 601 - EUR * 20
        assert(actualPrice)(equalTo(expectedPrice))
      },
    )

  private val toStringSuite =
    suite("to string")(
      test("simple") {
        assert((ARS * 100).toString)(equalTo("(ARS 100)")) &&
        assert((ARS * -100).toString)(equalTo("(ARS -100)"))
      },
      test("compound") {
        assert((ARS * 100 + EUR * 10).toString)(equalTo("(ARS 100) + (EUR 10)"))
      },
      test("complex") {
        assert((EUR * 10 + ARS * 100 + ARS * -10 + EUR * 1).toString)(equalTo("(ARS 90) + (EUR 11)"))
      },
    )

  private val fromStringSuite =
    suite("from string")(
      test("simple") {
        assert(Price.fromStringUnsafe("ARS 100"))(equalTo(ARS * 100)) &&
        assert(Price.fromStringUnsafe("(ARS 100)"))(equalTo(ARS * 100)) &&
        assert((ARS * -100).toString)(equalTo("(ARS -100)"))
      },
      test("compound") {
        assert(Price.fromStringUnsafe("(ARS 100) + (EUR 10)"))(equalTo(ARS * 100 + EUR * 10))
      },
      test("complex") {
        assert(Price.fromStringUnsafe("(ARS 100) + (EUR 10) + (ARS -10) + (EUR 1)"))(equalTo((ARS * 90) + (EUR * 11)))
      },
    )

  private val toStringIdentitySuite = {
    def toString: Price => String = _.toString
    def id                        = Price.fromStringUnsafe _ compose toString
    suite("to string andThen from string identity")( // this is not true the other way around !
      testM("single") {
        check(Gen.bigDecimal(-10, 10)) { amount =>
          val ars = ARS * amount
          assert(id(ars))(equalTo(ars))
        }
      },
      testM("compound") {
        check(Gen.bigDecimal(-10, 10), Gen.bigDecimal(-10, 10)) { (arsAmount, eurAmount) =>
          val ars = ARS * arsAmount
          val eur = EUR * eurAmount
          assert(id(ars + eur))(equalTo(ars + eur))
        }
      },
      testM("complex") {
        check(Gen.bigDecimal(-10, 10), Gen.bigDecimal(-10, 10), Gen.bigDecimal(-10, 10), Gen.bigDecimal(-10, 10)) {
          (ars1amount, eur1amount, ars2amount, eur2amount) =>
            val ars1 = ARS * ars1amount
            val eur1 = EUR * eur1amount
            val ars2 = ARS * ars2amount
            val eur2 = EUR * eur2amount
            assert(id(ars1 - eur1 + ars2 + eur2))(equalTo(ars1 - eur1 + ars2 + eur2))
        }
      },
    )
  }

  override def spec =
    suite("util")(
      singlePriceSuite,
      compoundPriceSuite,
      zeroPriceSuite,
      toStringSuite,
      fromStringSuite,
      toStringIdentitySuite,
    )

}
