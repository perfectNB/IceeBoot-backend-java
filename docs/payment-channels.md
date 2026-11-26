# 扫码收款

原有 YunGouOS 配置及创建二维码接口继续使用。可用 `YUNGOU_NOTIFY_URL` 覆盖原回调地址。新增官方渠道默认关闭，启用后出现在「支付配置 → 去支付」中。所有回调 URL 都应填写支付平台能够访问的 HTTPS 地址，包含部署时的 API 前缀。

| Spring 配置前缀 | 需要设置的属性 | 回调相对路径 |
| --- | --- | --- |
| `iceeboot.pay.wechat` | `enabled=true`、`app-id`、`merchant-id`、`merchant-serial`、`private-key-path`、`public-key-path`、`public-key-id`、`api-v3-key`、`notify-url` | `/v1/pay/notify/wechat` |
| `iceeboot.pay.alipay` | `enabled=true`、`app-id`、`seller-id`、`private-key`、`public-key`、`notify-url` | `/v1/pay/notify/alipay` |

微信使用 Native 支付和微信支付公钥模式。私钥路径指向商户私钥，公钥路径和 ID 来自微信支付平台。支付宝使用当面付预下单和 RSA2 公钥模式，分别提供应用私钥、支付宝公钥及商户 PID。通过部署环境注入密钥。

每笔新订单在创建时保存支付渠道和权益策略。支付通知通过 SDK 验签并核对商户、订单、金额后才确认收款。五种内置会员按订单永久发放一份，对应明细保存在 `t_pay_entitlement`。业务发放失败可在支付记录里重试。已有 YunGouOS 订单仍接受原回调并使用原商品配置。
