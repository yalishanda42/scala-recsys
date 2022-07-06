# Архитектура на препоръчваща система (Proof of Concept)
Александър Игнатов, 0MI3400082

## Цел
Настоящият проект има за цел да демонстрира използването на методите на функционалното програмиране, комбинирано с ООП, за изграждане на скалируема архитектура на препоръчваща система, използваща [Apache Spark](https://spark.apache.org/docs/latest/). Като пример са разработени и няколко модела върху наборите от данни [movelens-100k](https://grouplens.org/datasets/movielens/) и [goodbooks-10k](https://www.kaggle.com/datasets/zygmunt/goodbooks-10k?select=ratings.csv).

## Структура

```
src/main/scala
├── App.scala
├── domains
│   ├── books
│   │   ├── algorithms
│   │   │   └── BooksRecommenderV1.scala
│   │   └── datatransformers
│   │       └── BooksTransformer.scala
│   ├── movielens
│   │   ├── algorithms
│   │   │   ├── MovieRecommenderV1.scala
│   │   │   ├── MovieRecommenderV2.scala
│   │   │   └── MovieRecommenderV3.scala
│   │   └── datatransformers
│   │       └── MovieLensTransformer.scala
│   └── restaurants
│       ├── algorithms
│       │   └── RestaurantsRecommenderV1.scala
│       └── datatransformers
│           └── RestaurantsTransformer.scala
├── metrics
│   └── Metrics.scala
├── registry
│   └── AlgorithmsRegistry.scala
├── shared
│   ├── testables
│   │   └── MatrixFactorizationModelTester.scala
│   └── trainables
│       ├── ALSTrainer.scala
│       └── HyperALSTrainer.scala
├── traits
│   ├── Algorithm.scala
│   ├── DataTransformer.scala
│   ├── Testable.scala
│   └── Trainable.scala
└── utils
    ├── Logger.scala
    └── SparkProvider.scala
```

### Домейни

Модулите на системата са обособени по домейни, в случая: [books](src/main/scala/domains/books), [movielens](src/main/scala/domains/movielens), [restaurants](src/main/scala/domains/restaurants). Необходим е и модул [shared](src/main/scala/shared), съдържащ типове, използвани от повече от един домейн.

### Абстракции

В модула [traits](src/main/scala/traits) се намират основните абстракции, които имплементира и/или използва всеки домейн.

0. `Algorithm`

```scala
trait Algorithm[RowType, ModelType <: Saveable]:
  def transformer: DataTransformer[RowType]
  def trainer: Trainable[RowType, ModelType]
  def tester: Testable[RowType, ModelType]
```

Изисква да бъдат предоставени три обекта, които се използват за изграждане и тестване на моделите:

1. `DataTransformer`

```scala
case class Split[RowType](
  train: RDD[RowType],
  test: RDD[RowType]
)

trait DataTransformer[RowType]:
  def preprocess(data: RDD[String]): RDD[RowType]
  def split(data: RDD[RowType]): Split[RowType]
```

*  Методът `preprocess` дефинира процеса на предобработка на данните: системата подава `RDD` (пълният набор от данни), като целта е всеки ред да бъде конвертиран от прочетения `String` до даден `RowType` (в примерите в проекта е използван `Rating`).

* Методът `split` дефинира процеса на разделяне на данните на обучителни и тестови. Системата подава вече преработения от `preprocess` пълен набор от данни.

2. `Trainable`

```scala
trait Trainable[RowType, ModelType <: Saveable]:
  def train(data: RDD[RowType]): ModelType
```

Методът `train` използва данните за обучение (разделени от `DataTransformer[_].split`), за да създаде модел, който е готов да бъде съхранен на диск (поради това и необходимостта от `Saveable`), тестван и използван.

3. `Testable`

```scala
trait Testable[RowType, ModelType]:
   def test(model: ModelType, metric: Metric, actualData: RDD[RowType]): Double
```

Методът `test` използва модела (създаден от `Trainable[_, _].train`) и дадена метрика, за да измери качеството на модела върху даден набор от данни. Връща резултатът от `metric` при сравнение на `actualData` с `RDD`, получен при използването на модела.

### Метрики

```scala
sealed trait Metric:
  def evaluate(actualData: RDD[Rating], predictedData: RDD[Rating]): Double
```

В модулът [metrics](src/main/scala/metrics) се намират дефиниции на основните метрики, които използва и/или използва всеки домейн. Абстракцията `Metric` ни предоставя интерфейса за това.

За целите на демонстрацията за момента е имплементиран единствен неин наследник, `RMSE`, дефиниращ изчислението на средно-квадратична грешка:

$$ RMSE = \sqrt{\frac{1}{n}\sum_{i=1}^{n}(\hat{y}_i - y_i)^2} $$

## App.scala

В този файл е дефинирано примерно конзолно приложение, използващо дефинираната препоръчваща система.

Употребата от командния ред става по следния начин:

```
RecommenderApp algorithm train|test|predict [-u|-i <id>] dataPath modelBasePath
```
където:

* `algorithm` е името на алгоритъма, както е регистрирано в обекта `AlgorithmsRegistry` в модула  [registry](src/main/scala/registry)
* `train|test|predict` е избор от три възможни подкоманди:
  * `train` обучава алгоритъма, запазва го (в `modelBasePath`), зарежда го оттам и след това тества получения модел, логвайки резултатът от тестването. В началото се зарежда наборът от данни от `dataPath` и се разделя на обучителни и тестови.
  * `test` само зарежда (от `modelBasePath`) и тества модела върху пълния набор от данни, намиращ се в `dataPath`.
  * `predict -u|-i id` използва модела за генериране на 10 препоръки. При опция `-u` се препоръчват 10 най-подходящи потребители за продукт с id = `id`, а при `-i` - 10-те най-подходящи продукта за потребителя с id = `id`.


### `utils`

Модулът има помощна функция. Използва се единствено от конзолното приложение в App.scala.

1. `Logger`

Позволява писане на текст в STDOUT и STDERR чрез ефекта `IO`.

2. `SparkProvider`

Предоставя безопасен достъп до `SparkContext` чрез ефекта `Resource`.

## Примерни модели и експерименти

Дефинираните в проекта модели използват [алгоритъма Alternating Least Squares](https://dl.acm.org/doi/10.1109/MC.2009.263), чиято [имплементация](https://spark.apache.org/docs/latest/api/scala/org/apache/spark/mllib/recommendation/ALS.html) е предоставена от [библиотеката Spark](https://spark.apache.org/docs/latest/mllib-collaborative-filtering.html#collaborative-filtering).

1. `MovieRecommenderV1`

Трениран върху сплит 80%-20% на `movelens-100k` с параметри на ALS:
  * $ rank = 50 $
  * $ maxIterations = 20 $
  * $ \lambda = 0.01 $

Резултат върху тестови набор от данни: $ RMSE \approx 1.331 $

Резултат върху пълния набор от данни: $ RMSE \approx 1.354 $

2. `MovieRecommenderV2`

Трениран върху сплит 80%-20% на `movelens-100k` с параметри на ALS:
  * $ rank = 20 $
  * $ maxIterations = 10 $
  * $ \lambda = 0.05 $

Резултат върху тестови набор от данни: $ RMSE \approx 1.399 $

Резултат върху пълния набор от данни: $ RMSE \approx 1.409 $

3. `MovieRecommenderV3`

Трениран върху сплит 80%-20% на `movelens-100k` с оптимизация на хипермараметрите на ALS. Допълнително обучаващия набор от данни е разделен на 90%-10% за обучение и валидация съответно. Избран е моделът с най-добри резултати измежду следните стойности на хиперпараметрите:
  * $ rank \in \{ 10, 20, 25\} $
  * $ maxIterations \in \{ 5, 10, 20 \} $
  * $ \lambda \in \{ 0.01, 0.05, 0.1 \} $

Резултат върху тестови набор от данни: $ RMSE \approx 1.330 $

Резултат върху пълния набор от данни: $ RMSE \approx 1.343 $

4. `BooksRecommenderV1`

Трениран върху сплит 80%-20% на `goodbooks-10k` с параметри на ALS:
  * $ rank = 50 $
  * $ maxIterations = 20 $
  * $ \lambda = 0.01 $

Резултат върху тестови набор от данни: $ RMSE \approx 1.197 $

Резултат върху пълния набор от данни: $ RMSE \approx 1.197 $


