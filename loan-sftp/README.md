loan-sftp 实现一个高可用的sftp 服务
* 支持sftp客户端链接服务
* 高可用（多个服务实例同时支持）
* 保证文件一致性


使用开源项目  Spring Integration  FTP/FTPS 实现



* 接受客户端请求处理类 ：上传，下载，认证


上传文件：
先写入临时文件中，当判断最后一次写入时，就把临时文件移动到正式目录下

下载文件：
判断当前文件不存在，就从数据库中获取这个文件的真实服务器和下载地址。
读取时，从对应的服务器获取文件部分内容


- 命令行登录sftp服务
sftp -P 2222 admin@172.30.96.1
type:1  type:200  type:16

ls
type:11(SSH_FXP_OPENDIR)  type:12(SSH_FXP_READDIR)  type:4(SSH_FXP_CLOSE)


get /s3/file/1.txt


put ./2.txt /s3/file/
type=17 

put ./2.txt
type=3 typeName= open






acl,[BUILTIN\Administrators:READ_DATA/WRITE_DATA/APPEND_DATA/READ_NAMED_ATTRS/WRITE_NAMED_ATTRS/EXECUTE/DELETE_CHILD/READ_ATTRIBUTES/WRITE_ATTRIBUTES/DELETE/READ_ACL/WRITE_ACL/WRITE_OWNER/SYNCHRONIZE:ALLOW, NT AUTHORITY\SYSTEM:READ_DATA/WRITE_DATA/APPEND_DATA/READ_NAMED_ATTRS/WRITE_NAMED_ATTRS/EXECUTE/DELETE_CHILD/READ_ATTRIBUTES/WRITE_ATTRIBUTES/DELETE/READ_ACL/WRITE_ACL/WRITE_OWNER/SYNCHRONIZE:ALLOW, NT AUTHORITY\Authenticated Users:READ_DATA/WRITE_DATA/APPEND_DATA/READ_NAMED_ATTRS/WRITE_NAMED_ATTRS/EXECUTE/READ_ATTRIBUTES/WRITE_ATTRIBUTES/DELETE/READ_ACL/SYNCHRONIZE:ALLOW, BUILTIN\Users:READ_DATA/READ_NAMED_ATTRS/EXECUTE/READ_ATTRIBUTES/READ_ACL/SYNCHRONIZE:ALLOW]
archive,true
attributes,524320
creationTime,2025-08-04T02:50:22.0983764Z
fileKey,null
hidden,false
isDirectory,false
isOther,false
isRegularFile,true
isSymbolicLink,false
lastAccessTime,2025-08-05T10:17:18.4035846Z
lastModifiedTime,2025-08-04T02:58:18.3907234Z
owner,BUILTIN\Administrators (Alias)
permissions,[OWNER_READ, OWNER_WRITE, GROUP_READ, GROUP_WRITE, OTHERS_READ, OTHERS_WRITE]
readonly,false
size,285
system,false





打开文件，返回一个文件句柄（已经存储了一个文件通道）
读取文件，根据文件句柄，拿到存储的文件通道，根据文件通道实例获取文件内容（分片的内容）
返回分片内容
继续读取文件，直到文件读取完毕
关闭文件，根据文件句柄，拿到存储的文件通道，关闭文件通道




### minio
win 启动命令
* E:\ruanjian\minio.exe server  D:\work\sso
访问地址：http://127.0.0.1:9000


