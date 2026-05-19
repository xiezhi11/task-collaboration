CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(100) NOT NULL,
    task_title VARCHAR(200) NOT NULL,
    task_description TEXT,
    creator_id BIGINT NOT NULL,
    creator_name VARCHAR(50),
    executor_id BIGINT,
    executor_name VARCHAR(50),
    start_date DATE,
    end_date DATE,
    progress INT DEFAULT 0,
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    task_status VARCHAR(30) DEFAULT 'DRAFT',
    is_overdue INT DEFAULT 0,
    was_overdue INT DEFAULT 0,
    accept_result VARCHAR(20),
    deleted INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(50),
    operation_type VARCHAR(50) NOT NULL,
    operation_content TEXT,
    reject_reason TEXT,
    old_status VARCHAR(30),
    new_status VARCHAR(30),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_reject_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    reject_reason TEXT NOT NULL,
    reject_by_id BIGINT NOT NULL,
    reject_by_name VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
