-- Smart Krishi Database Schema
-- Production MySQL 8.0+ compatible

DROP TABLE IF EXISTS disease_records;
DROP TABLE IF EXISTS crop_recommendations;
DROP TABLE IF EXISTS farms;
DROP TABLE IF EXISTS farmer_profiles;
DROP TABLE IF EXISTS advisories;
DROP TABLE IF EXISTS market_prices;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_FARMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE farmer_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    village VARCHAR(100),
    district VARCHAR(100),
    state VARCHAR(100),
    land_area DOUBLE DEFAULT 0.0,
    soil_type VARCHAR(50) DEFAULT 'Alluvial',
    irrigation_type VARCHAR(50) DEFAULT 'Tube Well',
    primary_crop VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_profile_state_district (state, district)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE farms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    farmer_profile_id BIGINT NOT NULL,
    farm_name VARCHAR(100) NOT NULL,
    plot_number VARCHAR(50),
    area_acres DOUBLE NOT NULL,
    soil_type VARCHAR(50),
    water_source VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_farm_profile FOREIGN KEY (farmer_profile_id) REFERENCES farmer_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE crop_recommendations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    nitrogen DOUBLE NOT NULL,
    phosphorus DOUBLE NOT NULL,
    potassium DOUBLE NOT NULL,
    ph DOUBLE NOT NULL,
    temperature DOUBLE NOT NULL,
    humidity DOUBLE NOT NULL,
    rainfall DOUBLE NOT NULL,
    soil_type VARCHAR(50) NOT NULL,
    recommended_crop VARCHAR(100) NOT NULL,
    suitable_season VARCHAR(50),
    fertilizer_suggestion TEXT,
    growing_conditions TEXT,
    crop_details TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_crop_rec_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_rec_crop (recommended_crop)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE disease_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    crop_name VARCHAR(100),
    image_url VARCHAR(500),
    image_name VARCHAR(255),
    detected_disease VARCHAR(150) NOT NULL,
    confidence_score DOUBLE,
    symptoms TEXT,
    treatment TEXT,
    prevention TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_disease_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_disease_crop (crop_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE market_prices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    crop_name VARCHAR(100) NOT NULL,
    variety VARCHAR(100),
    market_name VARCHAR(150) NOT NULL,
    district VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    min_price DOUBLE NOT NULL,
    max_price DOUBLE NOT NULL,
    modal_price DOUBLE NOT NULL,
    unit VARCHAR(30) DEFAULT '₹/Quintal',
    price_date DATE NOT NULL,
    trend VARCHAR(20) DEFAULT 'STABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_market_crop (crop_name),
    INDEX idx_market_state_district (state, district),
    INDEX idx_market_date (price_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE advisories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    season VARCHAR(50),
    target_crop VARCHAR(100),
    summary TEXT,
    details TEXT NOT NULL,
    applicable_state VARCHAR(100) DEFAULT 'All India',
    official_link VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_advisory_category (category),
    INDEX idx_advisory_season (season)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
