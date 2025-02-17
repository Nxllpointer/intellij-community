import sys
from _typeshed import ReadableBuffer, WriteableBuffer
from collections.abc import Iterable
from typing import Any, SupportsInt, overload
from typing_extensions import TypeAlias

if sys.version_info >= (3, 8):
    from typing import SupportsIndex

    _FD: TypeAlias = SupportsIndex
else:
    _FD: TypeAlias = SupportsInt

_CMSG: TypeAlias = tuple[int, int, bytes]
_CMSGArg: TypeAlias = tuple[int, int, ReadableBuffer]

# Addresses can be either tuples of varying lengths (AF_INET, AF_INET6,
# AF_NETLINK, AF_TIPC) or strings (AF_UNIX).
_Address: TypeAlias = tuple[Any, ...] | str
_RetAddress: TypeAlias = Any
# TODO Most methods allow bytes as address objects

# ----- Constants -----
# Some socket families are listed in the "Socket families" section of the docs,
# but not the "Constants" section. These are listed at the end of the list of
# constants.
#
# Besides those and the first few constants listed, the constants are listed in
# documentation order.

has_ipv6: bool

# Per socketmodule.c, only these three families are portable
AF_UNIX: int
AF_INET: int
AF_INET6: int

SOCK_STREAM: int
SOCK_DGRAM: int
SOCK_RAW: int
SOCK_RDM: int
SOCK_SEQPACKET: int

if sys.platform == "linux":
    SOCK_CLOEXEC: int
    SOCK_NONBLOCK: int

# Address families not mentioned in the docs
AF_AAL5: int
AF_APPLETALK: int
AF_ASH: int
AF_ATMPVC: int
AF_ATMSVC: int
AF_AX25: int
AF_BRIDGE: int
AF_DECnet: int
AF_ECONET: int
AF_IPX: int
AF_IRDA: int
AF_KEY: int
AF_LLC: int
AF_NETBEUI: int
AF_NETROM: int
AF_PPPOX: int
AF_ROSE: int
AF_ROUTE: int
AF_SECURITY: int
AF_SNA: int
AF_SYSTEM: int
AF_UNSPEC: int
AF_WANPIPE: int
AF_X25: int

# The "many constants" referenced by the docs
SOMAXCONN: int
AI_ADDRCONFIG: int
AI_ALL: int
AI_CANONNAME: int
AI_DEFAULT: int
AI_MASK: int
AI_NUMERICHOST: int
AI_NUMERICSERV: int
AI_PASSIVE: int
AI_V4MAPPED: int
AI_V4MAPPED_CFG: int
EAI_ADDRFAMILY: int
EAI_AGAIN: int
EAI_BADFLAGS: int
EAI_BADHINTS: int
EAI_FAIL: int
EAI_FAMILY: int
EAI_MAX: int
EAI_MEMORY: int
EAI_NODATA: int
EAI_NONAME: int
EAI_OVERFLOW: int
EAI_PROTOCOL: int
EAI_SERVICE: int
EAI_SOCKTYPE: int
EAI_SYSTEM: int
INADDR_ALLHOSTS_GROUP: int
INADDR_ANY: int
INADDR_BROADCAST: int
INADDR_LOOPBACK: int
INADDR_MAX_LOCAL_GROUP: int
INADDR_NONE: int
INADDR_UNSPEC_GROUP: int
IPPORT_RESERVED: int
IPPORT_USERRESERVED: int
IPPROTO_AH: int
IPPROTO_BIP: int
IPPROTO_DSTOPTS: int
IPPROTO_EGP: int
IPPROTO_EON: int
IPPROTO_ESP: int
IPPROTO_FRAGMENT: int
IPPROTO_GGP: int
IPPROTO_GRE: int
IPPROTO_HELLO: int
IPPROTO_HOPOPTS: int
IPPROTO_ICMP: int
IPPROTO_ICMPV6: int
IPPROTO_IDP: int
IPPROTO_IGMP: int
IPPROTO_IP: int
IPPROTO_IPCOMP: int
IPPROTO_IPIP: int
IPPROTO_IPV4: int
IPPROTO_IPV6: int
IPPROTO_MAX: int
IPPROTO_MOBILE: int
IPPROTO_ND: int
IPPROTO_NONE: int
IPPROTO_PIM: int
IPPROTO_PUP: int
IPPROTO_RAW: int
IPPROTO_ROUTING: int
IPPROTO_RSVP: int
IPPROTO_SCTP: int
IPPROTO_TCP: int
IPPROTO_TP: int
IPPROTO_UDP: int
IPPROTO_VRRP: int
IPPROTO_XTP: int
IPV6_CHECKSUM: int
IPV6_DONTFRAG: int
IPV6_DSTOPTS: int
IPV6_HOPLIMIT: int
IPV6_HOPOPTS: int
IPV6_JOIN_GROUP: int
IPV6_LEAVE_GROUP: int
IPV6_MULTICAST_HOPS: int
IPV6_MULTICAST_IF: int
IPV6_MULTICAST_LOOP: int
IPV6_NEXTHOP: int
IPV6_PATHMTU: int
IPV6_PKTINFO: int
IPV6_RECVDSTOPTS: int
IPV6_RECVHOPLIMIT: int
IPV6_RECVHOPOPTS: int
IPV6_RECVPATHMTU: int
IPV6_RECVPKTINFO: int
IPV6_RECVRTHDR: int
IPV6_RECVTCLASS: int
IPV6_RTHDR: int
IPV6_RTHDRDSTOPTS: int
IPV6_RTHDR_TYPE_0: int
IPV6_TCLASS: int
IPV6_UNICAST_HOPS: int
IPV6_USE_MIN_MTU: int
IPV6_V6ONLY: int
IPX_TYPE: int
IP_ADD_MEMBERSHIP: int
IP_DEFAULT_MULTICAST_LOOP: int
IP_DEFAULT_MULTICAST_TTL: int
IP_DROP_MEMBERSHIP: int
IP_HDRINCL: int
IP_MAX_MEMBERSHIPS: int
IP_MULTICAST_IF: int
IP_MULTICAST_LOOP: int
IP_MULTICAST_TTL: int
IP_OPTIONS: int
IP_RECVDSTADDR: int
IP_RECVOPTS: int
IP_RECVRETOPTS: int
IP_RETOPTS: int
IP_TOS: int
IP_TRANSPARENT: int
IP_TTL: int
LOCAL_PEERCRED: int
MSG_BCAST: int
MSG_BTAG: int
MSG_CMSG_CLOEXEC: int
MSG_CONFIRM: int
MSG_CTRUNC: int
MSG_DONTROUTE: int
MSG_DONTWAIT: int
MSG_EOF: int
MSG_EOR: int
MSG_ERRQUEUE: int
MSG_ETAG: int
MSG_FASTOPEN: int
MSG_MCAST: int
MSG_MORE: int
MSG_NOSIGNAL: int
MSG_NOTIFICATION: int
MSG_OOB: int
MSG_PEEK: int
MSG_TRUNC: int
MSG_WAITALL: int
NI_DGRAM: int
NI_MAXHOST: int
NI_MAXSERV: int
NI_NAMEREQD: int
NI_NOFQDN: int
NI_NUMERICHOST: int
NI_NUMERICSERV: int
SCM_CREDENTIALS: int
SCM_CREDS: int
SCM_RIGHTS: int
SHUT_RD: int
SHUT_RDWR: int
SHUT_WR: int
SOL_ATALK: int
SOL_AX25: int
SOL_HCI: int
SOL_IP: int
SOL_IPX: int
SOL_NETROM: int
SOL_ROSE: int
SOL_SOCKET: int
SOL_TCP: int
SOL_UDP: int
SO_ACCEPTCONN: int
SO_BINDTODEVICE: int
SO_BROADCAST: int
SO_DEBUG: int
SO_DONTROUTE: int
SO_ERROR: int
SO_EXCLUSIVEADDRUSE: int
SO_KEEPALIVE: int
SO_LINGER: int
SO_MARK: int
SO_OOBINLINE: int
SO_PASSCRED: int
SO_PEERCRED: int
SO_PRIORITY: int
SO_RCVBUF: int
SO_RCVLOWAT: int
SO_RCVTIMEO: int
SO_REUSEADDR: int
SO_REUSEPORT: int
SO_SETFIB: int
SO_SNDBUF: int
SO_SNDLOWAT: int
SO_SNDTIMEO: int
SO_TYPE: int
SO_USELOOPBACK: int
TCP_CORK: int
TCP_DEFER_ACCEPT: int
TCP_FASTOPEN: int
TCP_INFO: int
TCP_KEEPCNT: int
TCP_KEEPIDLE: int
TCP_KEEPINTVL: int
TCP_LINGER2: int
TCP_MAXSEG: int
TCP_NODELAY: int
TCP_QUICKACK: int
TCP_SYNCNT: int
TCP_WINDOW_CLAMP: int
if sys.version_info >= (3, 7):
    TCP_NOTSENT_LOWAT: int
if sys.version_info >= (3, 11) and sys.platform == "darwin":
    TCP_CONNECTION_INFO: int

# Specifically-documented constants

if sys.platform == "linux":
    AF_CAN: int
    PF_CAN: int
    SOL_CAN_BASE: int
    SOL_CAN_RAW: int
    CAN_EFF_FLAG: int
    CAN_EFF_MASK: int
    CAN_ERR_FLAG: int
    CAN_ERR_MASK: int
    CAN_RAW: int
    CAN_RAW_ERR_FILTER: int
    CAN_RAW_FILTER: int
    CAN_RAW_LOOPBACK: int
    CAN_RAW_RECV_OWN_MSGS: int
    CAN_RTR_FLAG: int
    CAN_SFF_MASK: int

    CAN_BCM: int
    CAN_BCM_TX_SETUP: int
    CAN_BCM_TX_DELETE: int
    CAN_BCM_TX_READ: int
    CAN_BCM_TX_SEND: int
    CAN_BCM_RX_SETUP: int
    CAN_BCM_RX_DELETE: int
    CAN_BCM_RX_READ: int
    CAN_BCM_TX_STATUS: int
    CAN_BCM_TX_EXPIRED: int
    CAN_BCM_RX_STATUS: int
    CAN_BCM_RX_TIMEOUT: int
    CAN_BCM_RX_CHANGED: int

    CAN_RAW_FD_FRAMES: int

if sys.platform == "linux" and sys.version_info >= (3, 8):
    CAN_BCM_SETTIMER: int
    CAN_BCM_STARTTIMER: int
    CAN_BCM_TX_COUNTEVT: int
    CAN_BCM_TX_ANNOUNCE: int
    CAN_BCM_TX_CP_CAN_ID: int
    CAN_BCM_RX_FILTER_ID: int
    CAN_BCM_RX_CHECK_DLC: int
    CAN_BCM_RX_NO_AUTOTIMER: int
    CAN_BCM_RX_ANNOUNCE_RESUME: int
    CAN_BCM_TX_RESET_MULTI_IDX: int
    CAN_BCM_RX_RTR_FRAME: int
    CAN_BCM_CAN_FD_FRAME: int

if sys.platform == "linux" and sys.version_info >= (3, 7):
    CAN_ISOTP: int

if sys.platform == "linux" and sys.version_info >= (3, 9):
    CAN_J1939: int
    CAN_RAW_JOIN_FILTERS: int

    J1939_MAX_UNICAST_ADDR: int
    J1939_IDLE_ADDR: int
    J1939_NO_ADDR: int
    J1939_NO_NAME: int
    J1939_PGN_REQUEST: int
    J1939_PGN_ADDRESS_CLAIMED: int
    J1939_PGN_ADDRESS_COMMANDED: int
    J1939_PGN_PDU1_MAX: int
    J1939_PGN_MAX: int
    J1939_NO_PGN: int

    SO_J1939_FILTER: int
    SO_J1939_PROMISC: int
    SO_J1939_SEND_PRIO: int
    SO_J1939_ERRQUEUE: int

    SCM_J1939_DEST_ADDR: int
    SCM_J1939_DEST_NAME: int
    SCM_J1939_PRIO: int
    SCM_J1939_ERRQUEUE: int

    J1939_NLA_PAD: int
    J1939_NLA_BYTES_ACKED: int

    J1939_EE_INFO_NONE: int
    J1939_EE_INFO_TX_ABORT: int

    J1939_FILTER_MAX: int

if sys.platform == "linux" and sys.version_info >= (3, 10):
    IPPROTO_MPTCP: int

if sys.platform == "linux":
    AF_PACKET: int
    PF_PACKET: int
    PACKET_BROADCAST: int
    PACKET_FASTROUTE: int
    PACKET_HOST: int
    PACKET_LOOPBACK: int
    PACKET_MULTICAST: int
    PACKET_OTHERHOST: int
    PACKET_OUTGOING: int

if sys.platform == "linux":
    AF_RDS: int
    PF_RDS: int
    SOL_RDS: int
    RDS_CANCEL_SENT_TO: int
    RDS_CMSG_RDMA_ARGS: int
    RDS_CMSG_RDMA_DEST: int
    RDS_CMSG_RDMA_MAP: int
    RDS_CMSG_RDMA_STATUS: int
    RDS_CMSG_RDMA_UPDATE: int
    RDS_CONG_MONITOR: int
    RDS_FREE_MR: int
    RDS_GET_MR: int
    RDS_GET_MR_FOR_DEST: int
    RDS_RDMA_DONTWAIT: int
    RDS_RDMA_FENCE: int
    RDS_RDMA_INVALIDATE: int
    RDS_RDMA_NOTIFY_ME: int
    RDS_RDMA_READWRITE: int
    RDS_RDMA_SILENT: int
    RDS_RDMA_USE_ONCE: int
    RDS_RECVERR: int

if sys.platform == "win32":
    SIO_RCVALL: int
    SIO_KEEPALIVE_VALS: int
    SIO_LOOPBACK_FAST_PATH: int
    RCVALL_IPLEVEL: int
    RCVALL_MAX: int
    RCVALL_OFF: int
    RCVALL_ON: int
    RCVALL_SOCKETLEVELONLY: int

if sys.platform == "linux":
    AF_TIPC: int
    SOL_TIPC: int
    TIPC_ADDR_ID: int
    TIPC_ADDR_NAME: int
    TIPC_ADDR_NAMESEQ: int
    TIPC_CFG_SRV: int
    TIPC_CLUSTER_SCOPE: int
    TIPC_CONN_TIMEOUT: int
    TIPC_CRITICAL_IMPORTANCE: int
    TIPC_DEST_DROPPABLE: int
    TIPC_HIGH_IMPORTANCE: int
    TIPC_IMPORTANCE: int
    TIPC_LOW_IMPORTANCE: int
    TIPC_MEDIUM_IMPORTANCE: int
    TIPC_NODE_SCOPE: int
    TIPC_PUBLISHED: int
    TIPC_SRC_DROPPABLE: int
    TIPC_SUBSCR_TIMEOUT: int
    TIPC_SUB_CANCEL: int
    TIPC_SUB_PORTS: int
    TIPC_SUB_SERVICE: int
    TIPC_TOP_SRV: int
    TIPC_WAIT_FOREVER: int
    TIPC_WITHDRAWN: int
    TIPC_ZONE_SCOPE: int

if sys.platform == "linux":
    AF_ALG: int
    SOL_ALG: int
    ALG_OP_DECRYPT: int
    ALG_OP_ENCRYPT: int
    ALG_OP_SIGN: int
    ALG_OP_VERIFY: int
    ALG_SET_AEAD_ASSOCLEN: int
    ALG_SET_AEAD_AUTHSIZE: int
    ALG_SET_IV: int
    ALG_SET_KEY: int
    ALG_SET_OP: int
    ALG_SET_PUBKEY: int

if sys.platform == "linux" and sys.version_info >= (3, 7):
    AF_VSOCK: int
    IOCTL_VM_SOCKETS_GET_LOCAL_CID: int
    VMADDR_CID_ANY: int
    VMADDR_CID_HOST: int
    VMADDR_PORT_ANY: int
    SO_VM_SOCKETS_BUFFER_MAX_SIZE: int
    SO_VM_SOCKETS_BUFFER_SIZE: int
    SO_VM_SOCKETS_BUFFER_MIN_SIZE: int
    VM_SOCKETS_INVALID_VERSION: int

AF_LINK: int  # Availability: BSD, macOS

# BDADDR_* and HCI_* listed with other bluetooth constants below

SO_DOMAIN: int
SO_PASSSEC: int
SO_PEERSEC: int
SO_PROTOCOL: int
TCP_CONGESTION: int
TCP_USER_TIMEOUT: int

if sys.platform == "linux" and sys.version_info >= (3, 8):
    AF_QIPCRTR: int

# Semi-documented constants
# (Listed under "Socket families" in the docs, but not "Constants")

if sys.platform == "linux":
    # Netlink is defined by Linux
    AF_NETLINK: int
    NETLINK_ARPD: int
    NETLINK_CRYPTO: int
    NETLINK_DNRTMSG: int
    NETLINK_FIREWALL: int
    NETLINK_IP6_FW: int
    NETLINK_NFLOG: int
    NETLINK_ROUTE6: int
    NETLINK_ROUTE: int
    NETLINK_SKIP: int
    NETLINK_TAPBASE: int
    NETLINK_TCPDIAG: int
    NETLINK_USERSOCK: int
    NETLINK_W1: int
    NETLINK_XFRM: int

if sys.platform != "win32" and sys.platform != "darwin":
    # Linux and some BSD support is explicit in the docs
    # Windows and macOS do not support in practice
    AF_BLUETOOTH: int
    BTPROTO_HCI: int
    BTPROTO_L2CAP: int
    BTPROTO_RFCOMM: int
    BTPROTO_SCO: int  # not in FreeBSD

    BDADDR_ANY: str
    BDADDR_LOCAL: str

    HCI_FILTER: int  # not in NetBSD or DragonFlyBSD
    # not in FreeBSD, NetBSD, or DragonFlyBSD
    HCI_TIME_STAMP: int
    HCI_DATA_DIR: int

if sys.platform == "darwin":
    # PF_SYSTEM is defined by macOS
    PF_SYSTEM: int
    SYSPROTO_CONTROL: int

# ----- Exceptions -----

error = OSError

class herror(error): ...
class gaierror(error): ...

if sys.version_info >= (3, 10):
    timeout = TimeoutError
else:
    class timeout(error): ...

# ----- Classes -----

class socket:
    @property
    def family(self) -> int: ...
    @property
    def type(self) -> int: ...
    @property
    def proto(self) -> int: ...
    @property
    def timeout(self) -> float | None: ...
    def __init__(self, family: int = ..., type: int = ..., proto: int = ..., fileno: _FD | None = ...) -> None: ...
    def bind(self, __address: _Address | bytes) -> None: ...
    def close(self) -> None: ...
    def connect(self, __address: _Address | bytes) -> None: ...
    def connect_ex(self, __address: _Address | bytes) -> int: ...
    def detach(self) -> int: ...
    def fileno(self) -> int: ...
    def getpeername(self) -> _RetAddress: ...
    def getsockname(self) -> _RetAddress: ...
    @overload
    def getsockopt(self, __level: int, __optname: int) -> int: ...
    @overload
    def getsockopt(self, __level: int, __optname: int, __buflen: int) -> bytes: ...
    if sys.version_info >= (3, 7):
        def getblocking(self) -> bool: ...

    def gettimeout(self) -> float | None: ...
    if sys.platform == "win32":
        def ioctl(self, __control: int, __option: int | tuple[int, int, int] | bool) -> None: ...

    def listen(self, __backlog: int = ...) -> None: ...
    def recv(self, __bufsize: int, __flags: int = ...) -> bytes: ...
    def recvfrom(self, __bufsize: int, __flags: int = ...) -> tuple[bytes, _RetAddress]: ...
    if sys.platform != "win32":
        def recvmsg(self, __bufsize: int, __ancbufsize: int = ..., __flags: int = ...) -> tuple[bytes, list[_CMSG], int, Any]: ...
        def recvmsg_into(
            self, __buffers: Iterable[WriteableBuffer], __ancbufsize: int = ..., __flags: int = ...
        ) -> tuple[int, list[_CMSG], int, Any]: ...

    def recvfrom_into(self, buffer: WriteableBuffer, nbytes: int = ..., flags: int = ...) -> tuple[int, _RetAddress]: ...
    def recv_into(self, buffer: WriteableBuffer, nbytes: int = ..., flags: int = ...) -> int: ...
    def send(self, __data: ReadableBuffer, __flags: int = ...) -> int: ...
    def sendall(self, __data: ReadableBuffer, __flags: int = ...) -> None: ...
    @overload
    def sendto(self, __data: ReadableBuffer, __address: _Address) -> int: ...
    @overload
    def sendto(self, __data: ReadableBuffer, __flags: int, __address: _Address) -> int: ...
    if sys.platform != "win32":
        def sendmsg(
            self,
            __buffers: Iterable[ReadableBuffer],
            __ancdata: Iterable[_CMSGArg] = ...,
            __flags: int = ...,
            __address: _Address = ...,
        ) -> int: ...
    if sys.platform == "linux":
        def sendmsg_afalg(
            self, msg: Iterable[ReadableBuffer] = ..., *, op: int, iv: Any = ..., assoclen: int = ..., flags: int = ...
        ) -> int: ...

    def setblocking(self, __flag: bool) -> None: ...
    def settimeout(self, __value: float | None) -> None: ...
    @overload
    def setsockopt(self, __level: int, __optname: int, __value: int | bytes) -> None: ...
    @overload
    def setsockopt(self, __level: int, __optname: int, __value: None, __optlen: int) -> None: ...
    if sys.platform == "win32":
        def share(self, __process_id: int) -> bytes: ...

    def shutdown(self, __how: int) -> None: ...

SocketType = socket

# ----- Functions -----

if sys.version_info >= (3, 7):
    def close(__fd: _FD) -> None: ...

def dup(__fd: _FD) -> int: ...

# the 5th tuple item is an address
def getaddrinfo(
    host: bytes | str | None,
    port: bytes | str | int | None,
    family: int = ...,
    type: int = ...,
    proto: int = ...,
    flags: int = ...,
) -> list[tuple[int, int, int, str, tuple[str, int] | tuple[str, int, int, int]]]: ...
def gethostbyname(__hostname: str) -> str: ...
def gethostbyname_ex(__hostname: str) -> tuple[str, list[str], list[str]]: ...
def gethostname() -> str: ...
def gethostbyaddr(__ip_address: str) -> tuple[str, list[str], list[str]]: ...
def getnameinfo(__sockaddr: tuple[str, int] | tuple[str, int, int, int], __flags: int) -> tuple[str, str]: ...
def getprotobyname(__protocolname: str) -> int: ...
def getservbyname(__servicename: str, __protocolname: str = ...) -> int: ...
def getservbyport(__port: int, __protocolname: str = ...) -> str: ...
def ntohl(__x: int) -> int: ...  # param & ret val are 32-bit ints
def ntohs(__x: int) -> int: ...  # param & ret val are 16-bit ints
def htonl(__x: int) -> int: ...  # param & ret val are 32-bit ints
def htons(__x: int) -> int: ...  # param & ret val are 16-bit ints
def inet_aton(__ip_string: str) -> bytes: ...  # ret val 4 bytes in length
def inet_ntoa(__packed_ip: bytes) -> str: ...
def inet_pton(__address_family: int, __ip_string: str) -> bytes: ...
def inet_ntop(__address_family: int, __packed_ip: bytes) -> str: ...
def CMSG_LEN(__length: int) -> int: ...
def CMSG_SPACE(__length: int) -> int: ...
def getdefaulttimeout() -> float | None: ...
def setdefaulttimeout(__timeout: float | None) -> None: ...
def socketpair(__family: int = ..., __type: int = ..., __proto: int = ...) -> tuple[socket, socket]: ...

if sys.platform != "win32":
    def sethostname(__name: str) -> None: ...

# Windows added these in 3.8, but didn't have them before
if sys.platform != "win32" or sys.version_info >= (3, 8):
    def if_nameindex() -> list[tuple[int, str]]: ...
    def if_nametoindex(__name: str) -> int: ...
    def if_indextoname(__index: int) -> str: ...
