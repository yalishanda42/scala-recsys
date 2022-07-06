# Архитектура на препоръчваща система (Proof of Concept)
Александър Игнатов, 0MI3400082

## Цел
Настоящият проект има за цел да демонстрира използването на методите на функционалното програмиране за изграждане на скалируема архитектура на препоръчваща система, използваща [Apache Spark](https://spark.apache.org/docs/latest/). Като пример са разработени и няколко модела върху наборите от данни [movelens-100k](https://grouplens.org/datasets/movielens/) и [goodbooks-10k](https://www.kaggle.com/datasets/zygmunt/goodbooks-10k?select=ratings.csv).

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


