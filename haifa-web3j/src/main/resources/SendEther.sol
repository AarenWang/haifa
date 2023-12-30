// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;

contract SendEther {
    //构造函数加payable，方便创建合约时转入一笔ETH
    constructor() payable{

    }
    // amount单位是wei
    function sendViaTransfer(address payable _to, uint amount) public payable {
        // transfer()函数已经不在推荐用来发送Ether
        //_to.transfer(msg.value);
        _to.transfer(amount);
    }

    function sendViaSend(address payable _to) public payable {
        // 通过send()函数发送ether，返回布尔值表示成功或失败
        // 该函数不再推荐用来发送Ether
        bool sent = _to.send(msg.value);
        require(sent, "Failed to send Ether");
    }

    function sendViaCall(address payable _to) public payable {
        // 通过返回布尔值表示是否发送成功
        // 当前推荐使用call()函数来发送Ether
        (bool sent, bytes memory data) = _to.call{value: msg.value}("");
        require(sent, "Failed to send Ether");
    }
}
