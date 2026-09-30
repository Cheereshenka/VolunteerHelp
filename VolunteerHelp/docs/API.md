# API VolunteerHelp

## Аутентификация

1. `GET /api/auth/csrf`.
2. `POST /api/auth/login` с JSON `{"username":"customer","password":"customer"}`.
3. Использовать `JSESSIONID` и CSRF cookie/header для изменяющих запросов.

## Примеры

### Создание заявки

`POST /api/requests`

```json
{
  "category": "покупки",
  "description": "Купить продукты по списку",
  "address": "Центральный район, ул. Примерная, 10",
  "requestedDate": "2026-10-01",
  "startTime": "15:00",
  "endTime": "17:00"
}
```

### Поиск

`GET /api/volunteers?category=покупки&district=Центральный%20район&from=15:00&to=17:00&minRating=4`

### Бронирование

`POST /api/requests/1/bookings`

```json
{"volunteerId": 1}
```

### Начало и завершение

`POST /api/bookings/1/start`

`POST /api/bookings/1/complete`

```json
{"report":"Поручение выполнено, покупки переданы заказчику."}
```

### Отзыв

`POST /api/bookings/1/review`

```json
{"rating":5,"comment":"Всё выполнено вовремя."}
```

Ошибки возвращаются в формате RFC 9457 `ProblemDetail` с кодами 400, 403, 404 и 409.
