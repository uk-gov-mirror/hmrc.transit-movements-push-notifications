/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.transitmovementspushnotifications.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.FORBIDDEN
import play.api.test.Helpers.INTERNAL_SERVER_ERROR
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.OK
import play.api.test.Helpers.REQUEST_ENTITY_TOO_LARGE
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.transitmovementspushnotifications.config.Constants
import uk.gov.hmrc.transitmovementspushnotifications.generators.ModelGenerators
import uk.gov.hmrc.transitmovementspushnotifications.models.*
import uk.gov.hmrc.transitmovementspushnotifications.models.APIVersionHeader.V3_0
import uk.gov.hmrc.transitmovementspushnotifications.models.responses.BoxResponse
import uk.gov.hmrc.transitmovementspushnotifications.utils.GuiceWiremockSuite

import scala.concurrent.ExecutionContext.Implicits.global

class PushNotificationConnectorSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with GuiceWiremockSuite
    with ModelGenerators
    with OptionValues
    with ScalaCheckDrivenPropertyChecks {
  override protected def portConfigKey: Seq[String] = Seq("microservice.services.push-pull-notifications-api.port")

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  val version: APIVersionHeader = V3_0

  "PushNotificationConnector" - {

    "getBox" - {

      val boxId    = arbitraryBoxId.arbitrary.sample.get
      val clientId = "Client_123"

      "should return a BoxResponse when the pushPullNotification API returns 200 and valid JSON with BoxNameV3_0 for apiVersion 3_0" in {
        server.stubFor {
          get(urlPathEqualTo("/box"))
            .withQueryParam("boxName", equalTo(Constants.BoxNameV3_0))
            .withQueryParam("clientId", equalTo(clientId))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(s"""
                {
                  "boxId": "${boxId.value}",
                  "boxName":"${Constants.BoxNameV3_0}",
                  "boxCreator":{
                      "clientId": "$clientId"
                  }
                }
              """)
            )
        }

        val app = applicationBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[PushPullNotificationConnector]
          whenReady(connector.getBox(clientId, V3_0)) {
            result =>
              result mustEqual BoxResponse(boxId)

              server.verify(
                getRequestedFor(urlPathEqualTo("/box"))
                  .withQueryParam("boxName", equalTo(Constants.BoxNameV3_0))
                  .withQueryParam("clientId", equalTo(clientId))
              )
          }
        }

      }

      "should return failed future when the pushPullNotification API returns 404" in {
        server.stubFor {
          get(urlPathEqualTo("/box")).willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
        }

        val app = applicationBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[PushPullNotificationConnector]
          val result    = connector.getBox(clientId, version)

          await(
            result
              .map {
                _ => fail("This should not succeed")
              }
              .recover {
                case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
              }
          )
        }
      }

      "should return failed future when the pushPullNotification API returns 500" in {
        server.stubFor {
          get(urlPathEqualTo("/box")).willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
        }

        val app = applicationBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[PushPullNotificationConnector]
          val result    = connector.getBox(clientId, version)

          await(
            result
              .map {
                _ => fail("This should not succeed")
              }
              .recover {
                case UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _) =>
              }
          )
        }
      }
    }

    "getAllBoxes" - {
      lazy val boxIdList = Gen.listOfN(3, arbitrary[BoxResponse]).sample.get

      "should return list of box id when the pushPullNotification API returns 200 and valid JSON" in {
        server.stubFor {
          get(urlPathEqualTo("/box")).willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(boxIdList).toString())
          )
        }

        val app = applicationBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[PushPullNotificationConnector]
          whenReady(connector.getAllBoxes) {
            result =>
              result mustEqual boxIdList
          }
        }

      }

      "should return failed future when the pushPullNotification API returns 404" in {
        server.stubFor {
          get(urlPathEqualTo("/box")).willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
        }

        val app = applicationBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[PushPullNotificationConnector]
          val result    = connector.getAllBoxes

          await(
            result
              .map {
                _ => fail("This should not succeed")
              }
              .recover {
                case UpstreamErrorResponse(_, NOT_FOUND, _, _) =>
              }
          )
        }
      }

      "should return failed future when the pushPullNotification API returns 500" in {
        server.stubFor {
          get(urlPathEqualTo("/box")).willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
          )
        }

        val app = applicationBuilder.build()

        running(app) {
          val connector = app.injector.instanceOf[PushPullNotificationConnector]
          val result    = connector.getAllBoxes

          await(
            result
              .map {
                _ => fail("This should not succeed")
              }
              .recover {
                case UpstreamErrorResponse(_, INTERNAL_SERVER_ERROR, _, _) =>
              }
          )
        }
      }
    }

    "postNotification" - {

      "when called with a valid message notification with a body and box id that is in the database" - {
        "should return Unit () when the post is successful" in forAll(
          arbitrary[BoxId],
          arbitrary[MessageId],
          arbitrary[MovementId],
          Gen.alphaNumStr,
          arbitrary[MessageType],
          arbitrary[MovementType]
        ) {
          (boxId, messageId, movementId, body, messageType, movementType) =>
            // forAll only suppoorts six entries, so we need to do other arbitraries like this
            val enrollmentEori              = arbitrary[EORINumber].sample.get
            val messageNotificationWithBody = MessageReceivedNotification(
              messageUri = s"/customs/transits/movements/departures/${movementId.value}/messages/${messageId.value}",
              movementId = movementId,
              messageId = messageId,
              movementType = movementType,
              enrollmentEORINumber = Some(enrollmentEori),
              messageBody = Some(body),
              messageType = Some(messageType)
            )
            server.stubFor {
              post(urlPathEqualTo(s"/box/${boxId.value}/notifications"))
                .withRequestBody(
                  equalToJson(
                    s"""
                    |{
                    |   "messageUri": "/customs/transits/movements/departures/${movementId.value}/messages/${messageId.value}",
                    |   "notificationType": "MESSAGE_RECEIVED",
                    |   "${movementType.toString.toLowerCase}Id": "${movementId.value}",
                    |   "messageId": "${messageId.value}",
                    |   "messageBody": "$body",
                    |   "messageType": "${messageType.value}",
                    |   "enrollmentEORINumber": "${enrollmentEori.value}"
                    |}
                    |""".stripMargin
                  )
                )
                .willReturn(
                  aResponse()
                    .withStatus(OK)
                )
            }

            val app = applicationBuilder.build()
            running(app) {
              val connector = app.injector.instanceOf[PushPullNotificationConnector]
              whenReady(connector.postNotification(boxId, messageNotificationWithBody)) {
                _ mustEqual Right((): Unit)
              }
            }
        }
      }

      "when called with a valid message notification with no body and box id that is in the database" - {
        "should return Unit () when the post is successful" in forAll(
          arbitrary[BoxId],
          arbitrary[MessageId],
          arbitrary[MovementId],
          arbitrary[MessageType],
          arbitrary[MovementType],
          arbitrary[EORINumber]
        ) {
          (boxId, messageId, movementId, messageType, movementType, eori) =>
            val messageNotificationWithoutBody = MessageReceivedNotification(
              messageUri = s"/customs/transits/movements/departures/${movementId.value}/messages/${messageId.value}",
              movementId = movementId,
              messageId = messageId,
              movementType = movementType,
              enrollmentEORINumber = Some(eori),
              messageBody = None,
              messageType = Some(messageType)
            )
            server.stubFor {
              post(urlPathEqualTo(s"/box/${boxId.value}/notifications"))
                .withRequestBody(
                  equalToJson(
                    s"""
                     |{
                     |   "messageUri": "/customs/transits/movements/departures/${movementId.value}/messages/${messageId.value}",
                     |   "${movementType.toString.toLowerCase}Id": "${movementId.value}",
                     |   "messageId": "${messageId.value}",
                     |   "notificationType": "MESSAGE_RECEIVED",
                     |   "messageType": "${messageType.value}",
                     |   "enrollmentEORINumber": "${eori.value}"
                     |}
                     |""".stripMargin
                  )
                )
                .willReturn(
                  aResponse()
                    .withStatus(OK)
                )
            }

            val app = applicationBuilder.build()
            running(app) {
              val connector = app.injector.instanceOf[PushPullNotificationConnector]
              whenReady(connector.postNotification(boxId, messageNotificationWithoutBody)) {
                _ mustEqual Right((): Unit)
              }
            }
        }
      }

      for (error <- List(BAD_REQUEST, FORBIDDEN, NOT_FOUND, REQUEST_ENTITY_TOO_LARGE, INTERNAL_SERVER_ERROR))
        "when called with an invalid request" - {
          s"should return error an error response: $error" in forAll(
            arbitrary[BoxId],
            arbitrary[MessageId],
            arbitrary[MovementId],
            arbitrary[MessageType],
            arbitrary[MovementType],
            arbitrary[EORINumber]
          ) {
            (boxId, messageId, movementId, messageType, movementType, eori) =>
              val messageNotificationWithoutBody = MessageReceivedNotification(
                messageUri = s"/customs/transits/movements/departures/$movementId/messages/$messageId",
                movementId = movementId,
                messageId = messageId,
                movementType = movementType,
                enrollmentEORINumber = Some(eori),
                messageBody = None,
                messageType = Some(messageType)
              )

              server.stubFor {
                post(urlPathEqualTo(s"/box/${boxId.value}/notifications")).willReturn(
                  aResponse()
                    .withStatus(error)
                )
              }

              val app = applicationBuilder.build()
              running(app) {
                val connector = app.injector.instanceOf[PushPullNotificationConnector]
                whenReady(connector.postNotification(boxId, messageNotificationWithoutBody)) {
                  case Left(value)  => value.statusCode mustEqual error
                  case Right(value) => fail(s"There must not be a Right (got $value instead)")
                }
              }
          }
        }
    }

  }

}
