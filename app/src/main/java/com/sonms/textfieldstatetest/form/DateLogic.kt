package com.sonms.textfieldstatetest.form

/**
 * ISO 형식("yyyy-MM-dd") 문자열은 사전순 비교가 곧 날짜순 비교이므로,
 * 별도의 날짜 파싱 없이 종료일이 시작일보다 빠른지 판정한다.
 * 두 값 중 하나라도 비어 있으면 아직 검증할 수 없으므로 오류가 아니라고 본다.
 */
fun endBeforeStart(start: String, end: String): Boolean =
    start.isNotBlank() && end.isNotBlank() && end < start
