import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OS Command Injection 취약점 및 수정 검증 테스트
 *
 * [취약점 위치] LibraryManager.checkServerStatus()
 * [취약 원인]  사용자 입력 IP를 검증 없이 OS 명령어에 직접 결합
 * [수정 방법]  IP 형식 정규식 검증 + String 배열 방식 exec 적용
 *
 * @see LibraryManager#checkServerStatus(String)
 * @see <a href="https://github.com/spumesco/LibraryManagement26/issues/4">Issue #4: OS Command Injection 수정</a>
 */
public class osCommandInjectionTest {

    LibraryRepository repository = new LibraryRepository();
    LibraryManager manager = new LibraryManager(repository);

    @Test
    @DisplayName("취약점 확인: OS Command Injection으로 임의 파일 생성 시도")
    void osCommandInjectionAttackTest() {
        // Given: 정상 IP 뒤에 명령어 구분자(&&)로 파일 생성 명령어 주입
        // 취약한 실행 결과: cmd.exe /c ping -n 1 127.0.0.1 && echo hacked > vuln.txt
        String fileName = "vuln.txt";
        String payload = "127.0.0.1 && echo hacked > " + fileName;

        // When: 취약한 서버 진단 기능 실행
        manager.checkServerStatus(payload);

        // Then: 파일이 생성되었다면 OS Command Injection 취약점이 존재함을 증명
        File injectedFile = new File(fileName);
        boolean isVulnerable = injectedFile.exists();

        if (isVulnerable) {
            injectedFile.delete(); // 흔적 제거
        }

        assertTrue(isVulnerable, "취약점 발견: OS 명령어가 주입되어 임의의 파일이 생성되었습니다.");

        if (isVulnerable) {
            System.out.println("[경고] OS Command Injection 공격 성공: 서버 내에서 임의 명령어가 실행되었습니다.");
        }
    }

    @Test
    @DisplayName("수정 확인: OS Command Injection 차단 검증")
    void osCommandInjectionFixTest() {
        // Given: 동일한 공격 페이로드
        // 수정 후: IP 정규식 검증 실패 → 즉시 return
        // → "127.0.0.1 && echo hacked > ..."는 IP 형식이 아니므로 실행 차단
        String fileName = "vuln_fixed.txt";
        String payload = "127.0.0.1 && echo hacked > " + fileName;

        // When
        manager.checkServerStatus(payload);

        // Then: 파일이 생성되지 않았다면 취약점이 차단되었음을 증명
        File injectedFile = new File(fileName);
        boolean isVulnerable = injectedFile.exists();

        if (isVulnerable) {
            injectedFile.delete();
        }

        assertFalse(isVulnerable, "수정 확인: OS Command Injection이 차단되어 파일이 생성되지 않아야 합니다.");

        if (!isVulnerable) {
            System.out.println("[확인] OS Command Injection 공격 차단 성공: 임의 명령어 실행이 불가능합니다.");
        }
    }
}
