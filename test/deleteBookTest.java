import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class deleteBookTest {
        LibraryRepository repository = new LibraryRepository();

    @Test
    @DisplayName("DB 도서 개별 삭제 테스트 및 물리 쿼리 검증 (deleteBook)")
    void deleteBook() {
        // Given: 테스트용 도서 데이터를 먼저 DB에 저장하여 삭제할 환경을 구축한다
        int targetBookId = 500;
        Map<Integer, Book> testMap = new HashMap<>();
        testMap.put(targetBookId, new Book(targetBookId, "삭제 테스트용 도서", "삭제저자", true, "null"));
        repository.saveBooks(testMap);
        // 데이터가 정상적으로 삽입되었는지 1차 검증한다
        Map<Integer, Book> beforeDeleteMap = repository.loadBooks();
        assertTrue(beforeDeleteMap.containsKey(targetBookId), "삭제 전에 해당 도서가 DB에 먼저 존재해야 합니다.");
        // When: 작성한 삭제 메서드(deleteBook)를 실행하여 쿼리 삭제를 작동시킨다
        boolean isDeleted = repository.deleteBook(targetBookId);
        // Then: 삭제 결과 및 직접 쿼리 조회를 통한 물리 데이터 잔류 여부를 검증한다
        // 1. 메서드의 반환값이 true인지 확인한다 (영향을 받은 행이 있었는지 검증)
        assertTrue(isDeleted, "존재하는 도서를 삭제했으므로 메서드는 true를 반환해야 합니다.");
        // 2. [물리 쿼리 검증 추가] 직접 SELECT 쿼리를 날려 데이터가 진짜 지워졌는지 확인한다
        String verifySql = "SELECT COUNT(*) FROM books WHERE book_id = ?";
        int recordCount = -1;
        // 기존 clearTables()에 정의된 마리아DB 연결 정보를 그대로 활용하여 직접 조회 수행
        try (Connection conn = DriverManager.getConnection(
                "jdbc:mariadb://192.168.100.20:3306/library", "cjulib", "security");
             PreparedStatement pstmt = conn.prepareStatement(verifySql)) {

            pstmt.setInt(1, targetBookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    recordCount = rs.getInt(1); // 조건에 맞는 행의 개수를 가져옴
                }
            }
        } catch (SQLException e) {
            fail("테스트 직접 쿼리 검증 중 DB 에러 발생: " + e.getMessage());
        }
        // 3. 데이터베이스 카운트가 0이어야 삭제 쿼리가 완벽하게 반영된 것이다
        assertEquals(0, recordCount, "삭제 쿼리가 정상 작동했다면 해당 ID의 카운트는 0이어야 합니다.");
    }
}
