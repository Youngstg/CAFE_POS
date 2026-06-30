# ====================
# Stage 1: Dependencies
# ====================
FROM php:8.3-fpm-alpine AS deps

RUN apk add --no-cache \
    git \
    curl \
    libpng-dev \
    libxml2-dev \
    zip \
    unzip \
    oniguruma-dev \
    postgresql-dev

RUN docker-php-ext-install pdo pdo_mysql pdo_pgsql mbstring exif pcntl bcmath gd xml

COPY --from=composer:latest /usr/bin/composer /usr/bin/composer

WORKDIR /app
COPY apps/backend/composer.json apps/backend/composer.lock ./
RUN composer install --no-scripts --no-autoloader --no-interaction --prefer-dist

# ====================
# Stage 2: Application
# ====================
FROM php:8.3-fpm-alpine AS app

RUN apk add --no-cache \
    libpng-dev \
    libxml2-dev \
    oniguruma-dev \
    postgresql-dev \
    nginx \
    supervisor

RUN docker-php-ext-install pdo pdo_mysql pdo_pgsql mbstring exif pcntl bcmath gd xml

WORKDIR /var/www/html

COPY --from=deps /app/vendor ./vendor
COPY apps/backend/ .

RUN composer dump-autoload --optimize && \
    php artisan config:cache && \
    php artisan route:cache && \
    chown -R www-data:www-data /var/www/html/storage /var/www/html/bootstrap/cache

EXPOSE 9000
CMD ["php-fpm"]
