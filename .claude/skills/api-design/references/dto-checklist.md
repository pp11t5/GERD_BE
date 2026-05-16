# DTO Checklist

## DTO Kind
- request DTO인가
- response DTO인가
- repository projection DTO인가

## Type Choice
- 단순 불변 구조면 `data class`로 충분한가
- 계층형 응답이면 nested class가 더 자연스러운가
- setter 없이 생성 시점에 값이 확정되는가

## Creation Place
- 반복 매핑이면 converter로 올려야 하는가
- 단발성 조립이면 service에서 직접 생성해도 되는가
- 엔티티 생성/수정 로직이 DTO로 들어오지 않았는가

## Boundary
- API DTO와 repository DTO를 분리해야 하는가
- 외부 계약용 필드만 담고 있는가
