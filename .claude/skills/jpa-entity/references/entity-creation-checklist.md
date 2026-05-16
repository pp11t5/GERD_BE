# Entity Creation Checklist

## Before Create
- 생성 주체가 Service 또는 Converter인가
- 연관 엔티티 존재 여부를 조회했는가
- 소유권/권한 검증이 끝났는가
- 중복/상태/락 검증이 필요한가

## Create Pattern
- 단순 생성이면 정적 팩터리 사용
- 파라미터가 많거나 optional 값이 섞이면 builder 검토
- DTO에 `toEntity()`를 두지 않음
- JPA용 기본 생성 경로와 비즈니스 생성 경로를 분리함

## After Create
- 파생 필드와 기본값이 함께 맞춰졌는가
- 상태 변경은 setter 대신 도메인 메서드로 노출되는가
- 외래키 값만으로 가짜 연관 엔티티를 만들지 않았는가
