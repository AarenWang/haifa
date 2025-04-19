

```bash
# 安装命令行
brew tap web3j/web3j
brew install web3j


# 编译合约生成合约二进制字节码和abi,输出到build目录
solc --bin --abi Hello.sol -o build


```

得到两个文件：
Hello.bin：合约字节码

Hello.abi：合约方法签名描述

使用web3j生成 合约的Java wrapper代码
```
web3j generate solidity \
  -b src/main/resources/build/HelloWorld.bin \
  -a src/main/resources/build/HelloWorld.abi \
  -o src/main/java \
  -p me.aarenwang.web3j

```
生成的类名为HelloWorld