

## До нагрузочного тестирования и оптимизации

### Только PUT 
- replicas=2/3 https://overload.yandex.net/148545
- replicas=3/3 https://overload.yandex.net/148546

### Только GET 
- replicas=2/3 https://overload.yandex.net/148549
- replicas=3/3 https://overload.yandex.net/148552

### Смесь PUT/GET 
- replicas=2/3 https://overload.yandex.net/148565
- replicas=3/3 https://overload.yandex.net/148566


## После нагрузочного тестирования и оптимизации

### Только PUT  
- replicas=3/3 https://overload.yandex.net/148577

### Только GET  
- replicas=3/3 https://overload.yandex.net/148583

### Смесь PUT/GET
- replicas=2/3 https://overload.yandex.net/148599
- replicas=3/3 https://overload.yandex.net/148587


В качестве профайлера использовалась - jvisualvm
99% CPU уделялось на обслуживание методов one-nio сервера и библиотеки для работы с базой данных.

К основным оптимизацям можно отнести добавление кеширования часто используемых объектов и работу над DAO, 
параллельная отправка запросов к репликам.
В результате имеет место повышение количества обработанных запросов и RPS в целом. 
