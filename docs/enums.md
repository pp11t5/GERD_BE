# Enum 목록

API 요청·응답에서 사용하는 열거형 값 레퍼런스.

---

## JudgmentGrade — 신호등 판정 등급

> `com.gerd.domain.judgment.dto.enums.JudgmentGrade`

| 값 | 의미 |
|---|---|
| `RECOMMEND` | 추천 (🟢) |
| `CAUTION` | 주의 (🟡) |
| `RISK` | 위험 (🔴) |
| `UNKNOWN` | 판정 불가 (⚪) |

---

## TriggerCode — 트리거 음식

> `com.gerd.domain.food.entity.enums.TriggerCode`  
> JSON 직렬화 값은 `code` 컬럼 기준.

| 값 | code | 한글 |
|---|---|---|
| `CAFFEINE` | `caffeine` | 카페인 |
| `CARBONATED` | `carbonated` | 탄산음료 |
| `ALCOHOL` | `alcohol` | 알코올 |
| `SPICY` | `spicy` | 매운 음식 |
| `FRIED_FATTY` | `fried_fatty` | 튀긴·기름진 음식 |
| `CHOCOLATE` | `chocolate` | 초콜릿 |
| `CITRUS` | `citrus` | 감귤류 |
| `TOMATO` | `tomato` | 토마토 |
| `MINT` | `mint` | 민트 |
| `ONION_GARLIC_RAW` | `onion_garlic_raw` | 날 양파·마늘 |
| `CHEESE_DAIRY` | `cheese_dairy` | 치즈·유제품 |
| `REFINED_FLOUR` | `refined_flour` | 정제 밀가루 |

---

## AllergenCode — 알레르기

> `com.gerd.domain.food.entity.enums.AllergenCode`

| 값 | code | 한글 |
|---|---|---|
| `MILK` | `milk` | 우유 |
| `EGG` | `egg` | 달걀 |
| `WHEAT` | `wheat` | 밀 |
| `SOY` | `soy` | 대두 |
| `PEANUT` | `peanut` | 땅콩 |
| `CRUSTACEAN` | `crustacean` | 갑각류 |
| `TREE_NUT` | `tree_nut` | 견과류 |
| `FISH_SHELLFISH` | `fish_shellfish` | 생선·조개류 |

---

## FoodCategory — 음식 분류

> `com.gerd.domain.food.entity.FoodCategory` (DB 마스터 `food_categories`, 13종 고정)  
> enum이 아닌 시드 마스터 — JSON에는 `code`로 노출되며 `sort_order` 순서.

| code | 한글 |
|---|---|
| `rice_porridge` | 밥·죽 |
| `noodles` | 면류 |
| `bread_bakery` | 빵·베이커리 |
| `soup_stew` | 국·찌개·탕 |
| `grilled_jeon` | 구이·전 |
| `fried` | 튀김 |
| `stirfry_braise` | 볶음·조림 | 
| `steam_boil` | 찜·삶음 |
| `sashimi_sushi` | 회·초밥 |
| `salad_vegetable` | 샐러드·채소 |
| `snack_dessert` | 간식·디저트 |
| `fruit` | 과일 |
| `beverage` | 음료 |

---

## SymptomState — 증상 상태 (기록 시 선택값)

> `com.gerd.domain.symptom.entity.enums.SymptomState`

| 값 | code | 한글 |
|---|---|---|
| `COMFORTABLE` | `comfortable` | 편안해요 |
| `GOOD` | `good` | 양호해요 |
| `NORMAL` | `normal` | 보통이에요 |
| `UNCOMFORTABLE` | `uncomfortable` | 불편해요 |
| `SEVERE` | `severe` | 심각해요 |

---

## SymptomType — 증상 유형

> `com.gerd.domain.symptom.entity.enums.SymptomType`

| 값 | code | 한글 |
|---|---|---|
| `NONE` | `none` | 없음 |
| `THROAT_FOREIGN_BODY` | `throat_foreign_body` | 목 이물감이 있어요 |
| `ACID_REFLUX` | `acid_reflux` | 신물이 느껴져요 |
| `COUGH` | `cough` | 기침이 나요 |
| `CHEST_TIGHTNESS` | `chest_tightness` | 가슴이 답답해요 |

---

## SymptomCode — 온보딩 증상 체크리스트

> `com.gerd.domain.onboarding.entity.enums.SymptomCode`  
> 온보딩 06단계 다중선택 항목.

| 값 | code | 한글 |
|---|---|---|
| `HEARTBURN_REFLUX` | `heartburn_reflux` | 속쓰림·역류감 |
| `POST_MEAL_COUGH` | `post_meal_cough` | 식후 기침 |
| `THROAT_GLOBUS` | `throat_globus` | 목 이물감 |
| `SOUR_MOUTH_ODOR` | `sour_mouth_odor` | 신 맛·입냄새 |
| `SUPINE_CHEST_TIGHT` | `supine_chest_tight` | 누울 때 가슴 답답함 |
| `NONE_BUT_MANAGE` | `none_but_manage` | 증상 없지만 관리 목적 |

---

## NotificationSettingType — 알림 설정 유형

> `com.gerd.domain.notification.entity.enums.NotificationSettingType`

| 값 | code | 한글 |
|---|---|---|
| `POST_MEAL` | `post_meal` | 식후 알림 |
| `DAILY_RECORD` | `daily_record` | 일일 기록 알림 |
| `WEEKLY_REPORT` | `weekly_report` | 주간 리포트 알림 |

---

## DailyNotificationTime — 일일 알림 시각

> `com.gerd.domain.notification.entity.enums.DailyNotificationTime`

| 값 | code | 한글 |
|---|---|---|
| `MORNING_8` | `morning_8` | 오전 8시 |
| `EVENING_8` | `evening_8` | 오후 8시 |
| `NIGHT_9` | `night_9` | 오후 9시 |
| `NIGHT_10` | `night_10` | 오후 10시 |
