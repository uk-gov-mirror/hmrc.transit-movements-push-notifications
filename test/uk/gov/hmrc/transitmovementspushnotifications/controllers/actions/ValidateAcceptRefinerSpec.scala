/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.transitmovementspushnotifications.controllers.actions

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.http.HttpVerbs.POST
import play.api.http.Status
import play.api.test.Helpers.*
import play.api.test.Helpers.stubControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.transitmovementspushnotifications.base.TestActorSystem
import uk.gov.hmrc.transitmovementspushnotifications.models.APIVersionHeader
import uk.gov.hmrc.transitmovementspushnotifications.models.APIVersionHeader.V3_0

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ValidateAcceptRefinerSpec extends AnyFreeSpec with Matchers with ScalaFutures with TestActorSystem {

  implicit val ec: ExecutionContext = system.dispatcher

  val refiner = new ValidateAcceptRefiner(stubControllerComponents())

  "ValidateAcceptRefiner" - {

    "should reject request with missing APIVersion header" in {
      val request = FakeRequest(POST, "/").withBody(Json.obj())

      val resultF = refiner.refine(request)

      whenReady(resultF) {
        case Left(result) =>
          val futureResult = Future.successful(result)

          status(futureResult) mustEqual NOT_ACCEPTABLE
          contentAsJson(futureResult) mustEqual Json.obj(
            "message" -> "An APIVersion header is missing.",
            "code"    -> "NOT_ACCEPTABLE"
          )
        case Right(_) =>
          fail("Expected refiner to reject request")
      }
    }

    "should reject request with invalid APIVersion header" in {
      val request = FakeRequest(POST, "/")
        .withHeaders("APIVersion" -> "invalid-version")
        .withBody(Json.obj())

      val resultF = refiner.refine(request)

      whenReady(resultF) {
        case Left(result) =>
          val futureResult = Future.successful(result)
          status(futureResult) mustEqual UNSUPPORTED_MEDIA_TYPE
          contentAsJson(futureResult) mustEqual Json.obj(
            "message" -> "The APIVersion header invalid-version is not supported.",
            "code"    -> "UNSUPPORTED_MEDIA_TYPE"
          )
        case Right(_) =>
          fail("Expected refiner to reject request")
      }
    }

    "should accept request with valid APIVersion header" in {
      val request = FakeRequest(POST, "/")
        .withHeaders("APIVersion" -> "3.0")
        .withBody(Json.obj())

      val resultF = refiner.refine(request)

      whenReady(resultF) {
        case Right(validatedRequest) =>
          validatedRequest.versionHeader mustEqual V3_0
        case Left(_) =>
          fail("Expected refiner to accept request")
      }
    }
  }
}
