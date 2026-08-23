# FCM 리치 푸시 data-only payload

FCM 알림은 Android와 iOS 모두 `notification` 블록 없이 `data`만 전송한다. 앱이 `data.title`, `data.body`를 사용해 알림 UI와 액션을 직접 구성한다.

## 공통 data 필드

| key | type | required | example | note |
|---|---|---|---|---|
| `type` | string | O | `"post_meal"` | `NotificationType.code` |
| `title` | string | O | `"속은 좀 어떠세요?"` | 클라이언트 표시용 제목 |
| `body` | string | O | `"방금 드신 식사, 속은 좀 어떠세요? 지금 증상을 기록해 보세요."` | 클라이언트 표시용 본문 |
| `targetId` | string(UUID) | X | `"c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f"` | 식사 기록 외부 식별자. 대상이 없으면 키를 포함하지 않음 |
| `mealOccurredAt` | string | X | `"13:24"` | `post_meal` 낮 단건 알림의 식사 시각 (`HH:mm`) |
| `hoursElapsed` | string | X | `"6"` | `post_meal` 낮 단건 알림의 식후 경과 시간(정수 시간) |
| `foodNames` | string | X | `"된장찌개,잡곡밥"` | `post_meal` 낮 단건 알림의 음식명. 콤마 구분 문자열 |

`mealOccurredAt`, `hoursElapsed`, `foodNames`는 `post_meal` 낮 단건 알림에만 포함한다. 음식이 없거나 확인할 수 없으면 `foodNames`는 빈 문자열일 수 있다.

## 전송 방식

- Android: `AndroidConfig.Priority.HIGH`로 data-only 메시지를 전송한다.
- iOS: `apns-push-type=alert`, `apns-priority=10`으로 전송한다. `aps.alert`에 `title`, `body`를 넣고 `aps.category`에는 알림 `type`을 넣는다.
- iOS 앱은 `post_meal` category를 등록해 액션을 연결한다. 이때 `post_meal` 알림은 `aps.alert.title`, `aps.alert.body`로 시스템 알림이 표시된다.

## 메시지 예시

```json
{
  "data": {
    "type": "post_meal",
    "title": "속은 좀 어떠세요?",
    "body": "방금 드신 식사, 속은 좀 어떠세요? 지금 증상을 기록해 보세요.",
    "targetId": "c4e90e6a-2b3c-4d5e-8f90-1a2b3c4d5e6f",
    "mealOccurredAt": "13:24",
    "hoursElapsed": "6",
    "foodNames": "된장찌개,잡곡밥"
  }
}
```

## iOS APNs 예시

```json
{
  "message": {
    "token": "<fcm-token>",
    "apns": {
      "headers": {
        "apns-push-type": "alert",
        "apns-priority": "10"
      },
      "payload": {
        "aps": {
          "alert": {
            "title": "속은 좀 어떠세요?",
            "body": "방금 드신 식사, 속은 좀 어떠세요?"
          },
          "category": "post_meal"
        }
      }
    }
  }
}
```

## `post_meal_delayed_single`

### 야간 이연 식후 알림 (1건)

| 항목 | 내용 |
| --- | --- |
| 발송 시점 | 22:00~09:00 사이에 예정된 알림을 다음 날 09:00에 발송 |
| `targetId` | 식사 기록 외부 ID(UUID) |
| 이동 화면 | 해당 식사 기록의 증상 입력 화면 |
| title | 어젯밤 식사, 기록하셨나요? |
| body | 어젯밤 드신 식사, 속은 좀 어떠셨어요? 잊기 전에 기록해 보세요. |
| iOS APNs | `aps.alert`로 표시, `aps.category`는 `post_meal_delayed_single` |

## `post_meal_delayed_bulk`

### 야간 이연 식후 알림 (여러 건 묶음)

| 항목 | 내용 |
| --- | --- |
| 발송 시점 | 다음 날 09:00 |
| 발송 조건 | 미기록 식사가 2건 이상일 경우 |
| `targetId` | 없음 |
| 이동 화면 | 증상 미기록 목록 화면 |
| title | 미기록 식사가 있어요 |
| body | 어젯밤 식사 N건의 증상 기록이 남아 있어요. 잊기 전에 확인해 보세요. |
| iOS APNs | `aps.alert`로 표시, `aps.category`는 `post_meal_delayed_bulk` |

`N`은 실제 미기록 식사 개수로 동적으로 변경한다.

## `daily_record`

### 일일 식사 기록 알림

| 항목 | 내용 |
| --- | --- |
| 발송 시점 | 사용자가 설정한 시간 (기본값: 21:00) |
| `targetId` | 없음 |
| 이동 화면 | 식사 기록 입력 화면 |
| title | 오늘 식사 기록을 남겨보세요 |
| body | 오늘 하루 드신 식사를 기록하면 증상 패턴을 파악할 수 있어요. |
