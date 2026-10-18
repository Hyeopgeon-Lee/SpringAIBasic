-- Used only in the H2 integration test. Production Oracle tables are not created automatically.
CREATE TABLE STUDENT (
    USER_ID VARCHAR(100) PRIMARY KEY,
    USER_NAME VARCHAR(100),
    EMAIL VARCHAR(200),
    ADDR VARCHAR(200)
);
