-- MySQL
/**
  * @author nmng108
  */

CREATE DATABASE IF NOT EXISTS MICROTUBE;
USE MICROTUBE;

# DROP TABLE USER;
# DROP TABLE CHANNEL;
# DROP TABLE VIDEO;
# ALTER TABLE CHANNEL DROP CONSTRAINT fn_channel_user;
# ALTER TABLE VIDEO DROP CONSTRAINT fn_video_channel;

CREATE TABLE USER
(
    ID              BIGINT       NOT NULL AUTO_INCREMENT,
    USERNAME        VARCHAR(20)  NOT NULL UNIQUE,
    NAME            VARCHAR(255) NOT NULL,
    EMAIL           VARCHAR(100) NOT NULL,
    PHONE_NUMBER    VARCHAR(20),
    ADDITIONAL_INFO TEXT,
    CREATED_BY      BIGINT,
    CREATED_AT      TIMESTAMP    NOT NULL,
    MODIFIED_BY     BIGINT,
    MODIFIED_AT     TIMESTAMP,
    DELETED_AT      TIMESTAMP,
    PRIMARY KEY (ID)
);

CREATE TABLE CHANNEL
(
    ID          BIGINT       NOT NULL AUTO_INCREMENT,
    NAME        VARCHAR(255) NOT NULL,
    PATH_NAME   VARCHAR(255) NOT NULL UNIQUE, -- initially = USER.USERNAME
    DESCRIPTION TEXT,
    USER_ID     BIGINT       NOT NULL,
    CREATED_BY  BIGINT       NOT NULL,
    CREATED_AT  TIMESTAMP    NOT NULL,
    MODIFIED_BY BIGINT,
    MODIFIED_AT TIMESTAMP,
    DELETED_AT  TIMESTAMP,
    PRIMARY KEY (ID)
);
ALTER TABLE CHANNEL
    ADD CONSTRAINT fn_channel_user FOREIGN KEY (USER_ID) REFERENCES USER (ID) ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE TABLE VIDEO
(
    ID                BIGINT       NOT NULL AUTO_INCREMENT,
    NAME              VARCHAR(255) NOT NULL,
    DESCRIPTION       TEXT,
    VISIBILITY        INT          NOT NULL,
    CHANNEL_ID        BIGINT       NOT NULL,
    ORIGINAL_FILENAME VARCHAR(100),
    TEMP_FILEPATH     VARCHAR(1000),
    DEST_FILEPATH     VARCHAR(1000),
    RESOLUTION        VARCHAR(100), -- A comma-separated list of resolution names. Ex: 720p,1080p
    STATUS            INT,
    VIEW_COUNT        BIGINT DEFAULT 0,
    CREATED_BY        BIGINT       NOT NULL,
    CREATED_AT        TIMESTAMP    NOT NULL,
    MODIFIED_BY       BIGINT,
    MODIFIED_AT       TIMESTAMP,
    DELETED_AT        TIMESTAMP,
    PRIMARY KEY (ID)
);
ALTER TABLE VIDEO
    ADD CONSTRAINT fn_video_channel FOREIGN KEY (CHANNEL_ID) REFERENCES CHANNEL (ID) ON UPDATE CASCADE ON DELETE RESTRICT;
