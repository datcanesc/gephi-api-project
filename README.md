# Gephi Toolkit ve Spring Boot ile Sosyal Ağ Analizi

## Proje Genel Bakış  
Bu proje, **Gephi Toolkit 0.10.2** ve **Spring Boot (JDK 17)** kullanarak **GEXF** ve **GML** dosyalarından sosyal ağ grafikleri oluşturmak için geliştirilmiştir.  

Uygulama, yüklenen grafik dosyalarını işleyerek sosyal ağ analizi yapar ve grafik çıktıları üretir. Ek olarak, opsiyonel bir `filterGraph` metodu mevcuttur ve grafiklere filtre uygulanmasını sağlar. Bu metod ve ilgili kodlar **`GephiService.java`** içinde yorum satırı olarak bulunmaktadır. Filtreleme özelliğini etkinleştirmek için ilgili satırları açmanız gerekmektedir.  

Filtrelenmiş ve filtrelenmemiş çıktı örnekleri `SampleFiles` dizininde PDF formatında mevcuttur.  
8-11, 42, 108-123 burada GephiService.java dosyası içerisindeki bu satıları yorumdan çıkararak Filtereleme özelliğini etkinleştirebilirsiniz.

## Offline olarak kütüphaneleri bilgisayara indirme

İnternete bağlı olan bir bilgisayar ile aşağıdaki kodu çalıştırın. Bu kod projedeki bağımlılıkları libs klasörü içerisine kayıt JAR dosyası olarak kaydeder.(Bu komutu /backend klasörü içerisinde çalıştırın)
```sh
mvn dependency:copy-dependencies -DoutputDirectory=libs
```
Sonrasında offline olarak projenin çalıştırılmasını istediğiniz bilgisayara projeyi bir USB bellek veya başka bir aktarım yolu ile taşıyın.
Sonrasında aşağıdaki adımları izleyerek projeyi çalıştırabilirsiniz.

## Projeyi Çalıştırma  

1. Maven ile projeyi derleyin:  (/backend klasörü içerisinde yapılmalıdır)
   ```sh
   mvn clean install
   ```
2. Docker Compose kullanarak uygulamayı çalıştırın:  
   ```sh
   docker compose up --build
   ```

## API Testi  

Aşağıdaki `cURL` komutu ile uygulamayı test edebilirsiniz. Bu komut, bulunduğunuz dizinde `output.pdf` adlı bir dosya oluşturacaktır:  

```sh
curl -X POST "http://localhost:8080/api/graph/process" -H "Content-Type: multipart/form-data"  -F "file=@./SampleFiles/Java.gexf" -o output.pdf
```