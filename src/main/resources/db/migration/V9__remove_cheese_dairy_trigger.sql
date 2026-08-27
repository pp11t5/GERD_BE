-- L12: 유제품은 GERD 트리거 목록에서 제외한다. 기존 연결 데이터도 물리 삭제한다.
DELETE FROM food_triggers
WHERE trigger_label_id IN (SELECT trigger_label_id FROM trigger_labels WHERE code = 'cheese_dairy');

DELETE FROM user_triggers
WHERE trigger_label_id IN (SELECT trigger_label_id FROM trigger_labels WHERE code = 'cheese_dairy');

DELETE FROM trigger_labels
WHERE code = 'cheese_dairy';
