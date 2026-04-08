import sbt._

object AppDependencies {

  private val bootstrapPlayVersion = "10.7.0"
  private val catsVersion          = "2.13.0"
  private val mongoPlay            = "2.12.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30"    % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"           % mongoPlay,
    "io.lemonlabs"      %% "scala-uri"                    % "4.0.3",
    "org.typelevel"     %% "cats-core"                    % catsVersion,
    "uk.gov.hmrc"       %% "internal-auth-client-play-30" % "4.3.0"
  )

  val test = Seq(
    "org.apache.pekko"  %% "pekko-testkit"           % "1.0.3",
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapPlayVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % mongoPlay,
    "org.typelevel"     %% "cats-core"               % catsVersion,
    "org.scalatestplus" %% "mockito-5-12"            % "3.2.19.0",
    "org.scalacheck"    %% "scalacheck"              % "1.19.0",
    "org.typelevel"     %% "discipline-scalatest"    % "2.3.0"
  ).map(_ % Test)
}
