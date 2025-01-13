/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const useSelfReportHelper = () => {

  const REQUESTED = 'Approval Requested'
  const APPROVED = 'Approved'
  const REJECTED = 'Rejected'
  const AWAITING_GRADING = "Awaiting Grading"
  const PASSED = "Passed"
  const FAILED = "Failed"
  const COMPLETED = "Completed"

  const isApprovalRequest = (eventStatus) => {
    return eventStatus === REQUESTED
  }
  const isApproved = (eventStatus) => {
    return eventStatus === APPROVED
  }
  const isRejected = (eventStatus) => {
    return eventStatus === REJECTED
  }
  const isAwaitingGrading = (eventStatus) => {
    return eventStatus === AWAITING_GRADING
  }
  const isPassed = (eventStatus) => {
    return eventStatus === PASSED
  }
  const isFailed = (eventStatus) => {
    return eventStatus === FAILED
  }
  const isCompleted = (eventStatus) => {
    return eventStatus === COMPLETED
  }

  return {
    isApprovalRequest,
    isApproved,
    isRejected,
    isAwaitingGrading,
    isPassed,
    isFailed,
    isCompleted,
  }
}