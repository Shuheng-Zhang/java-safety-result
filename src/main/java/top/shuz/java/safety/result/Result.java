package top.shuz.java.safety.result;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <h3>结果封装容器</h3>
 *
 * <p>参照 Rust {@code Result<T, E>} 枚举设计</p>
 * <p>容器包含成功时的返回值 {@code T} 或失败时的错误(异常) {@code E}</p>
 * <p>该容器的设计旨在尽可能减少 {@code try-catch} 在业务逻辑中的耦合</p>
 *
 * @author heng
 * @since 2025/12/19
 */
public class Result<T, E> {

    /**
     * 成功时的返回值
     */
    private T value;
    /**
     * 失败时的错误内容
     */
    private E error;

    private Result() {}

    private Result(final  T value, final E error) {
        this. value = value;
        this.error = error;
    }

    /**
     * 创建成功值 {@code Ok(T)} 的封装
     */
    public static <T, E> Result<T, E> ok(final T value) {
        return new Result<>(value, null);
    }

    /**
     * 创建错误值(异常) {@code E(err)} 的封装
     */
    public static <T, E> Result<T, E> err(final E err) {
        if (err == null) throw new ResultException(ResultException.ERROR_NULL);

        return new Result<>(null, err);
    }

    /**
     * 检查返回值是否成功
     * @return {@code true} 正常返回值 {@code Ok(T)}, {@code false} 异常/错误 {@code Err(T)}
     */
    public boolean isOk() {
        return error == null;
    }


    /**
     * <p>解构 {@code Ok(T)} 中的值</p>
     * @return 成功时的返回值
     * @throws ResultException 当容器内 {@code Err(e)} 包含错误时将抛出异常
     */
    public T unwrap() {
        // 结果中包含错误
        if (error != null) {
            throw new ResultException(ResultException.VALUE_BUT_ERROR);
        }
        return value;
    }

    /**
     * <p>解构 {@code Err(e)} 中的错误信息</p>
     * @return 失败时的错误信息
     * @throws ResultException 当容器内 {@code Ok(T)} 包含成功返回值时抛出异常
     */
    public E unwrapError() {
        // 结果中包含成功的返回值
        if (error == null) {
            throw new ResultException(ResultException.ERROR_BUT_NULL);
        }
        return error;
    }

    /**
     * 将 {@code Ok(T)} 转换为 {@code Optional(T)}, 忽略可能存在的错误 {@code Err{e}}
     * @return Optional(T) 返回值的可空封装
     */
    public Optional<T> toOptional() {
        return isOk() ? Optional.ofNullable(value) : Optional.empty();
    }

    /**
     * <p>将成功值 {@code T} 应用 {@code mapper} 函数转换为 {@code U}, 并自动封装为 {@code Result<U, E>}</p>
     * <p>对错误值 {@code Err(e)} 将直接透传</p>
     * <p>在调用 {@code mapper} 函数时抛出的异常将会被捕获, 并以 {@code Err(e)} 返回</p>
     *
     * <p>此方法适用于纯数据转换(如类型转换、格式化)，不涉及可能主动返回错误场景</p>
     *
     * @param <U> 转换后的值类型
     * @param mapper 转换函数, 不可为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <U> Result<U, E> mapValue(final Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "[mapper] function cannot be NULL");

        if (!isOk()) {
            return Result.err(error);
        }

        try {
            final var result = mapper.apply(value);
            if (result == null) {
                return Result.err((E) new NullPointerException("Mapper cannot return an NULL result"));
            }

            return Result.ok(result);
        } catch (Throwable e) {
            return Result.err((E) e);
        }
    }

    /**
     * <p>对成功值 {@code T} 应用 {@code mapper} 函数转换为一个新的 {@code Result}, 并展平嵌套</p>
     * <p>对错误值 {@code Err(e)} 将直接透传</p>
     * <p>在调用 {@code mapper} 函数时抛出的异常将会被捕获, 并以 {@code Err(e)} 返回</p>
     *
     * <p>此方法适用于组合多个可能失败的操作(如调用另一个返回 {@code Result} 的方法), </p>
     * <p>避免产生 {@code Result<Result<U, E>, E>} 的嵌套</p>
     *
     * @param <U> 转换后 {@code Result} 中的值类型
     * @param mapper 转换函数, 不可为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <U> Result<U, E> mapResult(final Function<? super T, ? extends Result<U, E>> mapper) {
         Objects.requireNonNull(mapper, "[mapper] function cannot be NULL");

        if (!isOk()) {
            return Result.err(error);
        }

        try {
            final var result = mapper.apply(value);
            if (result == null) {
                return Result.err((E) new NullPointerException("Mapper cannot return an NULL result"));
            }
            return result;
        } catch (Throwable e) {
            return Result.err((E) e);
        }
    }

    /**
     * <p>对成功值 {@code T} 执行副作用操作而不影响值本身</p>
     * <p>忽略执行时可能抛出的异常</p>
     * @param peeker 副作用执行器
     */
    public Result<T, E> peek(final Consumer<? super  T> peeker) {
        return this.peek(peeker, null);
    }

    /**
     * <p>对成功值 {@code T} 执行副作用操作而不影响值本身</p>
     * @param peeker 副作用执行器
     * @param exceptionHandler 处理 {@code peeker} 执行时可能抛出的异常
     */
    public Result<T, E> peek(final Consumer<? super T> peeker, final Consumer<Throwable> exceptionHandler) {
        Objects.requireNonNull(peeker, "[peeker] function cannot be NULL");

        if (isOk()) {
            try {
                peeker.accept(value);
            } catch (Throwable t) {
                this.handleIgnoreException(exceptionHandler, t);
            }
        }
        return this;
    }

    /**
     * <p>终结消费 {@code Result} 成功时的值</p>
     * <p>忽略可能抛出的异常</p>
     */
    public void ifOK(final Consumer<? super T> consumer) {
        this.ifOk(consumer, null);
    }

    /**
     * <p>终结消费 {@code Result} 成功时的值</p>
     * @param consumer 消费执行器
     * @param exceptionHandler 处理 {@code consumer} 执行时可能抛出的异常
     */
    public void ifOk(final Consumer<? super T> consumer, final Consumer<Throwable> exceptionHandler) {
        if (isOk()) {
            try {
                consumer.accept(value);
            } catch (Throwable t) {
                this.handleIgnoreException(exceptionHandler, t);
            }
        }
    }

    /**
     * <p>终结消费 {@code Result} 错误时的值</p>
     * <p>忽略可能抛出的异常</p>
     */
    public void ifErr(final Consumer<? super E> consumer) {
        this.ifErr(consumer, null);
    }

    /**
     * <p>终结消费 {@code Result} 错误时的值</p>
     * @param consumer 消费执行器
     * @param exceptionHandler 处理 {@code consumer} 执行时可能抛出的异常
     */
    public void ifErr(final Consumer<? super E> consumer, final Consumer<Throwable> exceptionHandler) {
        if (!isOk()) {
            try {
                consumer.accept(error);
            } catch (Throwable t) {
                this.handleIgnoreException(exceptionHandler, t);
            }
        }
    }

    /**
     * <p>终结消费 {@code Result}</p>
     *
     * <p>必须处理 {@code Result} 的所有情况(成功/错误)</p>
     *
     * <p>忽略执行时可能抛出的异常</p>
     *
     * @param onOk 成功时的处理器
     * @param onErr 错误时的处理器
     */
    public void ifPresentOrElse(final Consumer<? super T> onOk, final Consumer<? super E> onErr) {
        this.ifPresentOrElse(onOk, onErr, null);
    }

    /**
     * <p>终结消费 {@code Result}</p>
     *
     * <p>必须处理 {@code Result} 的所有情况(成功/错误)</p>
     *
     * @param onOk 成功时的处理器
     * @param onErr 错误时的处理器
     * @param exceptionHandler 处理 {@code onOk} 或 {@code onErr} 可能抛出的异常
     */
    public void ifPresentOrElse(final Consumer<? super T> onOk, final Consumer<? super E> onErr, final Consumer<Throwable> exceptionHandler) {
        Objects.requireNonNull(onOk, "[onOk] function cannot be NULL");
        Objects.requireNonNull(onErr, "[onErr] function cannot be NULL");

        try {
            if (isOk()) {
                onOk.accept(value);
            } else {
                onErr.accept(error);
            }
        } catch (Throwable t) {
            this.handleIgnoreException(exceptionHandler, t);
        }
    }

    /**
     * 异常处理器, 忽略处理时可能二次抛出的异常
     */
    private void handleIgnoreException(final Consumer<Throwable> handler, final Throwable t) {
        Optional.ofNullable(handler).ifPresent(h -> {
            try {
                h.accept(t);
            } catch (Throwable e) {/* 防御性编码, 确保 exceptionHandler 不会传播异常 */}
        });
    }
}
