import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL Injection 취약점 및 수정 검증 테스트
 *
 * [취약점 위치] LibraryRepository.loadUser()
 * [취약 원인]  사용자 입력값을 SQL 쿼리에 직접 문자열 결합
 * [수정 방법]  PreparedStatement 파라미터 바인딩(?) 적용
 *
 * @see LibraryRepository#loadUser(String, String)
 * @see <a href="https://github.com/spumesco/LibraryManagement26/issues/3">Issue #3: SQL Injection 수정</a>
 */
public class sqlInjectionTest {

    LibraryRepository repository = new LibraryRepository();
    LibraryManager manager = new LibraryManager(repository);

    @Test
    @DisplayName("취약점 확인: SQL Injection으로 인증 우회 시도")
    void loginSqlInjectionAttackTest() {
        // Given: 비밀번호를 모르는 상태에서 항상 참이 되는 조건 주입
        // 주입 페이로드: ' OR 1=1 #
        // 취약한 쿼리 결과: SELECT * FROM users WHERE user_id = '' OR 1=1 #' AND password = '...'
        String attackId = "' OR 1=1 #";
        String attackPw = "wrong_password";

        // When: 취약한 login 메서드 호출
        boolean result = manager.login(attackId, attackPw);

        // Then: 취약한 상태에서는 로그인이 성공(true)되어야 취약점이 존재함을 증명
        assertTrue(result, "취약점 발견: SQL Injection 페이로드로 인증이 우회되었습니다.");

        if (result) {
            System.out.println("[경고] SQL Injection 공격 성공: 유효하지 않은 계정으로 로그인되었습니다.");
        }
    }

    @Test
    @DisplayName("수정 확인: SQL Injection 차단 검증")
    void loginSqlInjectionFixTest() {
        // Given: 동일한 공격 페이로드
        // 수정 후 쿼리: SELECT * FROM users WHERE user_id = ? AND password = ?
        // → 입력값이 SQL 문법으로 해석되지 않고 단순 문자열로 처리됨
        String attackId = "' OR 1=1 #";
        String attackPw = "wrong_password";

        // When
        boolean result = manager.login(attackId, attackPw);

        // Then: 수정 후에는 로그인이 실패(false)해야 취약점이 차단되었음을 증명
        assertFalse(result, "수정 확인: SQL Injection 페이로드가 차단되어 로그인에 실패해야 합니다.");

        if (!result) {
            System.out.println("[확인] SQL Injection 공격 차단 성공: 인증 우회가 불가능합니다.");
        }
    }
}
