# Minecraft Server Host

## Installation

### From Source

1. Download the source
2. Set the environment variable `HOST_INSTALL_DIR`
3. In the root directory, run the command `./gradlew installAndCopy`

<!---
## Remote API

The host is equipped with a remote API on port 8080.
The API makes use of Minecraft's packet encoding (see [wiki.vg][1]), though the packets themselves are different.

### Serverbound

#### Full Request

Packet ID: 0x00

No fields

Response:

* One General Response packet
* A number of Server Response packets equal to the number of servers defined in the GR packet.

#### General Request

Packet ID: 0x01

No fields

Response:

* One General Response packet

#### Server Request

Packet ID: 0x02

| Field Name | Field Type | Notes |
|------------|------------|-------|
| Server ID  | VarInt     |       |

Response:

* If server ID is valid, one Server Response packet
* If server ID is not valid, one Error packet

### Clientbound

#### Error

Packet ID: 0x00

| Field Name | Field Type |
|------------|------------|
| Reason     | String     |

#### General Response

Packet ID: 0x01

| Field Name   | Field Type | Notes                 |
|--------------|------------|-----------------------|
| Start Port   | VarInt     | The port of server 0  |
| Server Count | VarInt     | The number of servers |

#### Server Response

Packet ID: 0x02

| Field Name       | Field Type | Notes                    |
|------------------|------------|--------------------------|
| Server Name      | String     |                          |
| Server Directory | String     |                          |
| Combined State   | Byte       | Of the form `0b00UUUESS` |
| Timeout Duration | VarLong    |                          |

`UUU` is the timeout unit:

0b000: Milliseconds

0b001: Seconds

0b010: Minutes

0b011: Hours

0b100: Days

`E` is whether the timeout is enabled

`SS` is the server's state

0b00: Stopped

0b01: Started

0b10: Stopping

0b11: Errored

[1]: https://wiki.vg/Protocol "Minecraft's Protocol"
--->