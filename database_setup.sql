-- Run this once in PostgreSQL
CREATE DATABASE easyfish;

-- After creating the database, update src/main/resources/application.properties
-- and start the Spring Boot app. Hibernate ddl-auto=update will create/update tables:
-- users, product, order, order_item, delivery_address, product_image (if used by existing code)
