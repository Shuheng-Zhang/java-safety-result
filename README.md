# java-safety-result

更安全的返回结果封装


## 1. 设计

参照 ___Rust___ 的 `Result<T, E>` 枚举, 将一个方法的执行结果的值, 或执行过程出现的错误/异常统一使用 `Result<T, E>` 封装并返回.

调用者必须处理 `Result<T, E>` 可能存在的所有状态(成功或错误)才能获取到最终结果.

`Result<T, E>` 的模式旨在提供一种清晰的业务流程异常处理实现方案, 确保业务流程代码中不会受到 `try-catch` 的污染.


## 2. 功能

对结果(成功或错误)的封装方法：

+ [X] `Result.Ok(value)`: 封装成功时的值
+ [X] `Result.err(error)`: 封装错误结果 

支持链式调用的转换/消费方法:

+ [X] `mapValue(mapperFn)` / `mapValue(mapperFn, exceptionHandler)`: 对成功值的转换并自动封装 `Result`
+ [X] `mapResult(mapperFn)` / `mapResult(mapperFn, exceptionHandler)`: 对结果进行转换, 对可能出现的错误封装到到新的 `Result`
+ [X] `peek(peeker)` / `peek(peeker, exceptionHandler)`: 对成功值执行副作用操作而不影响值本身
+ [X] `ifOk(consumer)` / `ifOk(consumer, exceptionHander)`: 对成功值进行最终消费
+ [X] `ifErr(consumer)` / `ifErr(consumer, exceptionHandler)`: 对错误进行最终处理
+ [X] `ifPresentOrElse(consumer)` / `ifPresentOrElse(consumer, exceptionHandler)`: 终结消费, 必须处理 `Result` 的所有情况


## 3. 使用方法

最低 JDK 要求为 ___21___

1. 同步代码到本地:
   ```shell
   git clone https://github.com/Shuheng-Zhang/java-safety-result.git
   ```
2. 执行以下 `Maven` 指令:
    ```shell
    mvn clean install
    ```
3. 在项目内引用包:
    ```xml
    <dependency>
      <groupId>top.shuz</groupId>
      <artifactId>java-safety-result</artifactId>
      <version>0.1.0</version>
    </dependency>
    ```