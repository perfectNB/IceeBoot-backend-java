# 显式指定现有完整工具镜像，保留浏览器、Node 与已有 CLI。
ARG BASE_IMAGE
FROM ${BASE_IMAGE} AS wheels
COPY requirements.txt /tmp/requirements.txt
RUN python3 -m pip wheel --no-deps --wheel-dir /tmp/wheels -r /tmp/requirements.txt

FROM ${BASE_IMAGE}
COPY --from=wheels /tmp/wheels /tmp/iceeboot-wheels
RUN python3 -m pip install --no-index --no-cache-dir /tmp/iceeboot-wheels/*.whl \
    && python3 -c "from lunar_python import Solar; assert Solar.fromYmd(2026, 1, 1).getYear() == 2026" \
    && rm -rf /tmp/iceeboot-wheels
