import socket
import time
import sys

def send_syslog_udp(message, host='localhost', port=514, facility=1, severity=6):
    """通过UDP发送RFC 5424格式的Syslog消息"""
    # 计算优先级
    priority = facility * 8 + severity
    timestamp = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    hostname = socket.gethostname()
    app_name = "PythonApp"
    proc_id = str(os.getpid())
    msg_id = "ID001"

    # 构建RFC 5424消息
    syslog_msg = f"<{priority}>1 {timestamp} {hostname} {app_name} {proc_id} {msg_id} - {message}\n"

    # 创建UDP socket并发送消息
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.sendto(syslog_msg.encode('utf-8'), (host, port))
    sock.close()

    print(f"已通过UDP发送: {syslog_msg}")

def send_syslog_tcp(message, host='localhost', port=514, facility=1, severity=6):
    """通过TCP发送RFC 5424格式的Syslog消息"""
    # 计算优先级
    priority = facility * 8 + severity
    timestamp = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    hostname = socket.gethostname()
    app_name = "PythonApp"
    proc_id = str(os.getpid())
    msg_id = "ID001"

    # 构建RFC 5424消息
    syslog_msg = f"<{priority}>1 {timestamp} {hostname} {app_name} {proc_id} {msg_id} - {message}\n"

    # 创建TCP socket并发送消息
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        sock.connect((host, port))
        sock.sendall(syslog_msg.encode('utf-8'))
        print(f"已通过TCP发送: {syslog_msg}")
    except Exception as e:
        print(f"TCP发送失败: {e}")
    finally:
        sock.close()

if __name__ == "__main__":
    import os

    # 默认消息或从命令行获取消息
    message = sys.argv[1] if len(sys.argv) > 1 else "这是一条测试消息"
    host = sys.argv[2] if len(sys.argv) > 2 else "localhost"
    port = int(sys.argv[3]) if len(sys.argv) > 3 else 514

    # 同时使用UDP和TCP发送
    send_syslog_udp(message, host, port)
    #send_syslog_tcp(message, host, port)