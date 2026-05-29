<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:1E40AF,100:10B981&height=200&section=header&text=LocalService&fontSize=72&fontColor=ffffff&fontAlignY=38&desc=Yerel%20Hizmet%20%26%20Randevu%20Ekosistemi&descSize=20&descAlignY=58&descColor=e2e8f0" />

<br/>

[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_4.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis_7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

<br/>

[![Next.js](https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white)](https://nextjs.org/)
[![React Native](https://img.shields.io/badge/React_Native-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://reactnative.dev/)
[![Cloudflare R2](https://img.shields.io/badge/Cloudflare_R2-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)](https://developers.cloudflare.com/r2/)
[![Firebase](https://img.shields.io/badge/Firebase_FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)

<br/><br/>

> **Türkiye odaklı, genel kategorili yerel hizmet keşif ve randevu platformu.**
> Berberden veterinere, güzellik salonundan psikologlara — her randevu tabanlı işletme için tek ekosistem.

<br/>

[![CI](https://img.shields.io/github/actions/workflow/status/yunusemrebutu/localservice/ci.yml?branch=main&style=flat-square&label=CI%20Build&logo=github-actions)](https://github.com/yunusemrebutu/localservice/actions)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](CONTRIBUTING.md)

</div>

---

## 📌 İçindekiler

- [Proje Hakkında](#-proje-hakkında)
- [Özellikler](#-özellikler)
- [Mimari](#-mimari)
- [Teknoloji Stack](#-teknoloji-stack)
- [Başlangıç](#-başlangıç)
- [API Dokümantasyonu](#-api-dokümantasyonu)
- [Proje Yapısı](#-proje-yapısı)
- [Ortam Değişkenleri](#-ortam-değişkenleri)
- [Test](#-test)
- [Deployment](#-deployment)
- [Yol Haritası](#-yol-haritası)

---

## 🎯 Proje Hakkında

**LocalService**, Türkiye'deki randevu tabanlı yerel işletmeleri ve müşterileri buluşturan kapsamlı bir dijital ekosistemdir.

```
Kullanıcı randevu almak ister
        ↓
LocalService üzerinden yakındaki işletmeleri keşfeder
        ↓
Hizmet seçer → Personel seçer → Slot seçer
        ↓
Randevu onaylanır → Bildirim gelir → Randevuya gider
        ↓
Tamamlandıktan sonra yorum bırakır
```

### Hedef Kategoriler

| Kategori | Durum |
|----------|-------|
| 💈 Berber | ✅ Pilot |
| 💇 Kuaför | ✅ Pilot |
| 💅 Güzellik Salonu | ✅ Pilot |
| 💅 Tırnak Stüdyosu | ✅ Pilot |
| 🎨 Dövme Stüdyosu | ✅ Pilot |
| 🐾 Veteriner | 🔄 Faz 16 |
| 🏥 Klinik | 🔄 Faz 16 |
| 🥗 Diyetisyen | 🔄 Faz 16 |
| 🧠 Psikolog | 🔄 Faz 16 |
| 💪 Spor Merkezi | 🔄 Faz 16 |
| ⚽ Halı Saha | 🔄 Faz 16 |

---

## ✨ Özellikler

<table>
<tr>
<td width="50%">

### 👤 Kullanıcı
- 📱 Telefon numarasıyla kayıt ve giriş
- 🔍 Konum bazlı işletme keşfi
- 🗺️ Harita görünümü
- 📅 Online randevu alma
- 🔔 Anlık push bildirimler
- ⭐ Tamamlanan randevu sonrası yorum
- 💳 Depozito ile güvenli ödeme
- 📱 iOS & Android native uygulama

</td>
<td width="50%">

### 🏢 İşletme
- 🎛️ Kapsamlı işletme paneli
- 👥 Personel ve hizmet yönetimi
- 🗓️ Randevu takvimi
- ⏰ Çalışma saati yönetimi
- 📊 İş analitikleri
- 📸 Fotoğraf galerisi
- 📱 Mobil randevu takibi
- 💬 Müşteri iletişimi

</td>
</tr>
<tr>
<td width="50%">

### 🛡️ Güvenlik & Altyapı
- 🔐 JWT + Refresh Token (SHA-256)
- 🔄 Otomatik token rotasyonu
- 🔒 Redis blacklist
- 📵 OTP telefon doğrulama
- ⚡ Redis slot lock (çift rezervasyon önleme)
- 🛡️ Role bazlı yetkilendirme

</td>
<td width="50%">

### 🤖 Gelişmiş Özellikler
- 🧠 Spring AI entegrasyonu
- 🔔 Firebase Cloud Messaging
- ⚙️ n8n otomasyon akışları
- 💰 iyzico/PayTR ödeme
- 🌍 Çoklu şehir desteği
- 📈 Yatırımcı metrikleri

</td>
</tr>
</table>

---

## 🏗️ Mimari

```
┌─────────────────────────────────────────────────────────────┐
│                        İstemciler                           │
│                                                             │
│   Next.js PWA          React Native         Business Panel  │
│   (Kullanıcı Web)      (Mobil Uygulama)     (React + Vite) │
└─────────────────┬───────────────┬───────────────┬───────────┘
                  │               │               │
                  └───────────────┼───────────────┘
                                  │ REST API
                  ┌───────────────▼───────────────┐
                  │     Spring Boot 4.x Backend    │
                  │     (Modüler Monolith)          │
                  │                                │
                  │  ┌──────────┐ ┌─────────────┐ │
                  │  │   Auth   │ │  Randevu    │ │
                  │  │  Module  │ │   Motoru    │ │
                  │  └──────────┘ └─────────────┘ │
                  │  ┌──────────┐ ┌─────────────┐ │
                  │  │ Business │ │  Discovery  │ │
                  │  │  Module  │ │   Module    │ │
                  │  └──────────┘ └─────────────┘ │
                  │  ┌──────────┐ ┌─────────────┐ │
                  │  │ Payment  │ │     AI      │ │
                  │  │  Module  │ │   Module    │ │
                  │  └──────────┘ └─────────────┘ │
                  └───┬───────────────┬────────────┘
                      │               │
          ┌───────────▼───┐   ┌───────▼───────┐
          │  PostgreSQL 16 │   │    Redis 7    │
          │                │   │               │
          │  Ana Veri DB   │   │  Cache / Lock │
          │  TIMESTAMPTZ   │   │  OTP / Session│
          └───────────────┘   └───────────────┘
                      │
          ┌───────────▼───────────┐
          │    Cloudflare R2      │
          │   Object Storage      │
          │  (Fotoğraf / Medya)   │
          └───────────────────────┘
```

### Çift Rezervasyon Önleme

```
Kullanıcı slot seçer
        │
        ▼
┌───────────────────┐     ✅ Boşsa
│  Redis Slot Lock  │──────────────► Geçici kilit koy (5 dk)
│  (Katman 1)       │                        │
└───────────────────┘                        ▼
        │ ❌ Dolu                  Randevu oluştur
        ▼                                    │
  409 Conflict                               ▼
                               ┌─────────────────────────┐
                               │  PostgreSQL Unique       │
                               │  Constraint (Katman 2)   │
                               │  (staff_id, start_time)  │
                               └─────────────────────────┘
                                            │ ❌ Çakışma
                                            ▼
                                      409 Conflict
```

---

## 🛠️ Teknoloji Stack

### Backend
| Teknoloji | Versiyon | Kullanım |
|-----------|----------|----------|
| Java | 21 LTS | Ana dil |
| Spring Boot | 4.x | Framework |
| Spring Security | — | Auth & RBAC |
| Spring Data JPA | — | ORM |
| Spring Data Redis | — | Cache & Lock |
| Spring AI | — | AI entegrasyonu (Faz 14) |
| JJWT | 0.12.6 | JWT token yönetimi |
| Lombok | — | Boilerplate azaltma |

### Veritabanı & Cache
| Teknoloji | Versiyon | Kullanım |
|-----------|----------|----------|
| PostgreSQL | 16 | Ana veritabanı |
| Redis | 7 | Cache, OTP, slot lock, blacklist |
| Flyway | — | DB migration (Faz 9+) |
| PostGIS | — | Gelişmiş konum sorguları (Faz 17+) |

### Storage & Messaging
| Teknoloji | Kullanım |
|-----------|----------|
| Cloudflare R2 | Fotoğraf & medya storage |
| AWS SDK v2 | R2 istemcisi |
| Netgsm | SMS & OTP |
| Firebase FCM | Push bildirimler (Faz 10+) |
| n8n | Otomasyon akışları (Faz 10+) |

### Frontend & Mobile
| Teknoloji | Kullanım |
|-----------|----------|
| Next.js + PWA | Kullanıcı web uygulaması |
| React + Vite + Tailwind | Business & Admin panel |
| React Native (Bare) | Kullanıcı mobil uygulaması |
| SpringDoc OpenAPI 2.8.6 | API dokümantasyonu |

### DevOps
| Teknoloji | Kullanım |
|-----------|----------|
| Docker + Docker Compose | Container yönetimi |
| GitHub Actions | CI/CD pipeline |
| VPS | Production deployment |

---

## 🚀 Başlangıç

### Ön Gereksinimler

```bash
# Gerekli araçlar
java --version      # Java 21+
mvn --version       # Maven 3.8+
docker --version    # Docker 24+
```

### Hızlı Başlatma

```bash
# 1. Repoyu klonla
git clone https://github.com/yunusemrebutu/localservice.git
cd localservice

# 2. PostgreSQL ve Redis'i başlat
docker-compose up -d postgres redis

# 3. Uygulamayı dev modunda başlat
mvn spring-boot:run -Dspring.profiles.active=dev

# 4. Swagger UI'a git
open http://localhost:8080/swagger-ui.html
```

### Docker ile Tam Başlatma

```bash
# Tüm servisleri başlat (app + postgres + redis)
docker-compose up -d

# Logları izle
docker-compose logs -f app

# Durdur
docker-compose down
```

### İlk Admin Girişi

```
POST http://localhost:8080/api/v1/auth/login

{
  "phone": "05000000000",
  "password": "Admin@12345"
}
```

> ⚠️ Production ortamında `DataInitializer`'daki admin şifresini mutlaka değiştirin.

---

## 📡 API Dokümantasyonu

Swagger UI: `http://localhost:8080/swagger-ui.html`

### Auth Endpoint'leri

```http
POST   /api/v1/auth/register      # Kayıt
POST   /api/v1/auth/login         # Giriş
POST   /api/v1/auth/refresh       # Token yenileme
POST   /api/v1/auth/logout        # Çıkış
GET    /api/v1/auth/me            # Profil bilgisi
```

### Kullanıcı Endpoint'leri

```http
GET    /api/v1/users/profile          # Profil görüntüle
PUT    /api/v1/users/profile          # Profil güncelle
POST   /api/v1/users/change-password  # Şifre değiştir
POST   /api/v1/users/verify-phone     # Telefon doğrula
DELETE /api/v1/users/account          # Hesap sil
```

### İşletme Endpoint'leri

```http
POST   /api/v1/businesses             # İşletme oluştur
GET    /api/v1/businesses/{id}        # İşletme detayı
PUT    /api/v1/businesses/{id}        # İşletme güncelle
GET    /api/v1/businesses/my          # Kendi işletmem
GET    /api/v1/businesses/search      # Keşfet & filtrele
GET    /api/v1/businesses/nearby      # Yakındaki işletmeler
```

### Randevu Endpoint'leri

```http
GET    /api/v1/appointments/slots     # Müsait slotları listele
POST   /api/v1/appointments           # Randevu al
GET    /api/v1/appointments           # Randevularım
PUT    /api/v1/appointments/{id}/cancel    # İptal et
PUT    /api/v1/appointments/{id}/confirm  # Onayla (İşletme)
PUT    /api/v1/appointments/{id}/complete # Tamamla (İşletme)
```

### Admin Endpoint'leri

```http
GET    /api/v1/admin/users                        # Kullanıcılar
PATCH  /api/v1/admin/users/{id}/role              # Rol güncelle
PATCH  /api/v1/admin/users/{id}/deactivate        # Pasifleştir
GET    /api/v1/admin/businesses/pending           # Bekleyen işletmeler
PATCH  /api/v1/admin/businesses/{id}/approve      # Onayla
PATCH  /api/v1/admin/businesses/{id}/reject       # Reddet
```

---

## 📁 Proje Yapısı

```
src/main/java/com/yunus/
│
├── 📦 appointment/
│   ├── entity/          # Appointment, AppointmentStatus
│   └── repository/      # AppointmentRepository
│
├── 📦 auth/
│   ├── dto/             # LoginRequest, RegisterRequest, AuthResponse...
│   ├── AuthController   # /api/v1/auth/**
│   ├── AuthService      # Kayıt, giriş, çıkış akışları
│   ├── RefreshTokenService  # Token yaşam döngüsü
│   └── AdminUserController  # /api/v1/admin/users/**
│
├── 📦 business/
│   ├── entity/          # Business, BusinessCategory, BusinessPhoto...
│   └── repository/
│
├── 📦 common/
│   ├── entity/          # BaseEntity (UUID id, OffsetDateTime timestamps)
│   ├── exception/       # BusinessException, ConflictException...
│   └── response/        # BaseResponse<T>
│
├── 📦 config/
│   ├── SecurityConfig   # Spring Security, JWT filter chain
│   ├── SwaggerConfig    # OpenAPI 2.8.6
│   ├── DataInitializer  # Admin seed
│   └── RedisConfig      # RedisTemplate<String, String>
│
├── 📦 exception/
│   ├── ErrorType        # Hata kategorisi enum
│   ├── ErrorResponse    # Standart hata yanıtı
│   └── GlobalExceptionHandler  # @RestControllerAdvice
│
├── 📦 location/
│   ├── entity/          # City, District, Neighborhood
│   └── repository/
│
├── 📦 security/
│   ├── UserPrincipal    # Spring Security UserDetails
│   ├── CurrentUserService   # Oturum açık kullanıcı
│   ├── CustomUserDetailsService
│   ├── JwtFilter        # OncePerRequestFilter
│   ├── JwtService       # Token üretme & doğrulama
│   └── jwt/JwtProperties    # @ConfigurationProperties
│
├── 📦 sms/              # Netgsm entegrasyonu, OTP
├── 📦 storage/          # Cloudflare R2, AWS SDK v2
└── 📦 user/
    ├── entity/          # User, UserRole, RefreshToken
    └── repository/
```

---

## ⚙️ Ortam Değişkenleri

### `application-dev.yml` (Development)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/localservice
    username: localservice
    password: localservice
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

app:
  jwt:
    secret: your-256-bit-secret-key-here
    access-token-expiration: 900000      # 15 dakika
    refresh-token-expiration: 604800000  # 7 gün
  storage:
    endpoint: https://<account-id>.r2.cloudflarestorage.com
    bucket-name: localservice-dev
    access-key: your-r2-access-key
    secret-key: your-r2-secret-key
    public-url: https://pub-xxx.r2.dev
  sms:
    netgsm:
      username: your-netgsm-username
      password: your-netgsm-password
      header: LOCALSERVICE
  business:
    auto-approve: true  # Dev'de otomatik onay
```

### `application-prod.yml` (Production)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: none

app:
  jwt:
    secret: ${JWT_SECRET}
  storage:
    access-key: ${R2_ACCESS_KEY}
    secret-key: ${R2_SECRET_KEY}
  business:
    auto-approve: false  # Prod'da admin onayı zorunlu
```

---

## 🧪 Test

```bash
# Tüm testleri çalıştır
mvn test

# Belirli bir test sınıfı
mvn test -Dtest=JwtServiceTest

# Test raporu
mvn surefire-report:report
open target/site/surefire-report.html
```

### Test Kapsamı

| Test | Kapsam |
|------|--------|
| `JwtServiceTest` | Token üretme, doğrulama, expiry |
| `AuthServiceTest` | Register duplicate kontrolü, login akışı |
| `GlobalExceptionHandlerTest` | Validation response formatı |

---

## 🚢 Deployment

### Production Docker Compose

```bash
# Production build
docker build -t localservice:latest .

# Tüm servisleri production modunda başlat
SPRING_PROFILES_ACTIVE=prod docker-compose up -d

# Health check
curl http://localhost:8080/actuator/health
```

### GitHub Actions CI/CD

```yaml
# .github/workflows/ci.yml
# Tetikleyiciler: push → main, develop | pull_request → main, develop
# Adımlar: Java 21 setup → Maven cache → mvn clean verify
```

Her push'ta otomatik olarak:
1. ✅ Proje derlenir
2. ✅ Testler koşturulur
3. ✅ Build başarısız olursa merge engellenir

---

## 🗺️ Yol Haritası

```
Faz 0  ✅  Mimari kararlar ve proje dokümanları
Faz 1  ✅  Spring Boot altyapısı, JWT, Security, Storage, SMS/OTP
Faz 2  🔄  Kullanıcı profil yönetimi + İşletme kayıt sistemi
Faz 3  📋  İşletme yönetimi (hizmet, personel, fotoğraf, çalışma saatleri)
Faz 4  📋  Randevu motoru + Redis slot lock
Faz 5  📋  Keşif, harita, filtreleme, yorum sistemi
Faz 6  📋  Admin backend + Rate limiting
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       🎯  BACKEND MVP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Faz 7  📋  Business & Admin Panel (React + Vite)
Faz 8  📋  Kullanıcı Next.js PWA
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
       🎯  GERÇEK MVP
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Faz 9  📋  Pilot test + KVKK + İlk işletme onboarding
Faz 10 📋  Bildirim sistemi (FCM) + n8n otomasyonları
Faz 11 📋  Ödeme sistemi (iyzico/PayTR) + Depozito
Faz 12 📋  React Native kullanıcı mobil uygulaması
Faz 13 📋  İşletme mobil uygulaması
Faz 14 📋  AI özellikleri (Spring AI)
Faz 15 📋  Premium + Abonelik + Gelişmiş istatistikler
Faz 16 📋  Kategoriye özel modüller (Veteriner, Klinik, Spor...)
Faz 17 📋  Çok şehir + PostGIS + Ölçekleme + Flyway
Faz 18 📋  Yatırımcıya hazırlık + Metrik altyapısı
```

---

## 🔑 Rol Sistemi

```
ADMIN
  └── Platform yönetimi, işletme onayı, kullanıcı yönetimi

BUSINESS_OWNER
  └── İşletme profili, hizmetler, personel, randevular

BUSINESS_EMPLOYEE
  └── Randevu görüntüleme, takvim, müşteri iletişimi

USER
  └── Keşif, randevu alma, yorum, profil
```

---

## 🗄️ Veritabanı Şeması (Özet)

```
users ──────────────────────── refresh_tokens
  │
  ├── businesses ─────────────── business_category_map ── business_categories
  │       │
  │       ├── business_photos
  │       ├── services ──────── staff_services ── staff
  │       └── working_hours / holidays
  │
  └── appointments ──────────── reviews
          │
          └── payments
```

> 📌 Tüm `id` alanları `UUID`, tüm zaman alanları `OffsetDateTime` (TIMESTAMPTZ).

---

## 🤝 Katkıda Bulunma

1. Fork'la
2. Feature branch oluştur (`git checkout -b feature/amazing-feature`)
3. Commit'le (`git commit -m 'feat: add amazing feature'`)
4. Push'la (`git push origin feature/amazing-feature`)
5. Pull Request aç

---

## 📄 Lisans

Bu proje MIT lisansı altında dağıtılmaktadır. Detaylar için [LICENSE](LICENSE) dosyasına bakın.

---

<div align="center">

**Geliştirici:** [Yunus Emre Bütün](https://github.com/yunusemrebutu)

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:10B981,100:1E40AF&height=100&section=footer" />

</div>
