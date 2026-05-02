# DICOM Conformance Statement

| Field | Value |
|---|---|
| Vendor | Weasis Team |
| Software | Weasis DICOM Viewer |
| Version | 4.7.0 |
| Date | 2026-05-02 |

---

## 1 Implementation Model

Weasis is a multi-platform desktop DICOM viewer built on the Eclipse Equinox OSGi framework. It uses the dcm4che3 library (DICOM toolkit) for DICOM network operations and OpenCV (via JavaCPP) for image decompression and processing.

### 1.1 Application Data Flow Diagram

```
+------------------------------------------------------------------+
|                       Weasis DICOM Viewer                         |
|                                                                   |
|  +------------------+  +------------------+  +-----------------+  |
|  | DicomExplorer    |  | DicomQrView      |  | SendDicomView   |  |
|  | (Local Import)   |  | (Query/Retrieve)  |  | (Send/Export)   |  |
|  +--------+---------+  +--------+---------+  +--------+--------+  |
|           |                     |                     |           |
|  +--------v---------+  +--------v---------+  +--------v--------+  |
|  | DicomMediaIO     |  | C-FIND / C-MOVE  |  | C-STORE /       |  |
|  | (File I/O)       |  | C-GET / WADO     |  | STOW-RS         |  |
|  +------------------+  +------------------+  +------------------+  |
|                                                                   |
|  +------------------+  +------------------+  +-----------------+  |
|  | SR Viewer        |  | RT Viewer        |  | Waveform Viewer |  |
|  | (SR SpecialElem) |  | (RT SpecialElem) |  | (WF SpecialElem)|  |
|  +------------------+  +------------------+  +-----------------+  |
|                                                                   |
|  +------------------+  +------------------+  +-----------------+  |
|  | Audio Viewer     |  | 3D VolumeRender  |  | Encaps Doc View |  |
|  | (AU SpecialElem) |  | (MPR/VR)         |  | (PDF/Text)      |  |
|  +------------------+  +------------------+  +-----------------+  |
+------------------------------------------------------------------+
           |                         |                    |
           v                         v                    v
    +-----------+            +-------------+       +-----------+
    | DICOM     |            | DICOMWeb    |       | DICOM     |
    | Filesystem|            | (HTTP/HTTPS)|       | Print     |
    +-----------+            +-------------+       | SCP       |
                                                   +-----------+
```

### 1.2 Functional Overview

Weasis supports the following DICOM functionalities:

- **Local File Import**: Reading DICOM Part 10 files from local filesystem or removable media
- **DICOM Query/Retrieve** (SCU): C-FIND, C-MOVE, C-GET over DICOM network protocol
- **DICOM Storage** (SCU/SCP): C-STORE for sending objects; DICOM listener (SCP) for receiving
- **DICOMWeb** (Client): QIDO-RS for query, WADO-RS/WADO-URI for retrieve, STOW-RS for upload
- **DICOM Print Management** (SCU): Basic Grayscale and Color Print
- **Image Display & Processing**: Multiplanar reconstruction (MPR), Volume Rendering (3D)
- **Specialized Viewers**: Structured Reports (SR), Radiotherapy Objects (RT STRUCT, RT PLAN, RT DOSE), Waveform (ECG), Audio (AU), Encapsulated Documents (PDF, CDA), Segmentation (SEG)
- **Presentation State**: Grayscale Softcopy Presentation State (GSPS) rendering

---

## 2 Application Entity Specifications

### 2.1 Application Entity: WEASIS_AE

Weasis acts as a DICOM Application Entity with the configurable AE Title `weasis.aet` (default: `WEASIS_AE`).

#### 2.1.1 SOP Classes for Query/Retrieve (SCU)

Weasis implements C-FIND, C-MOVE, and C-GET as Query/Retrieve SCU.

| SOP Class Name | SOP Class UID | C-FIND | C-MOVE | C-GET |
|---|---|---|---|---|
| Patient Root Query/Retrieve Information Model - FIND | 1.2.840.10008.5.1.4.1.2.1.1 | Yes | — | — |
| Patient Root Query/Retrieve Information Model - MOVE | 1.2.840.10008.5.1.4.1.2.1.2 | — | Yes | — |
| Patient Root Query/Retrieve Information Model - GET | 1.2.840.10008.5.1.4.1.2.1.3 | — | — | Yes |
| Study Root Query/Retrieve Information Model - FIND | 1.2.840.10008.5.1.4.1.2.2.1 | Yes | — | — |
| Study Root Query/Retrieve Information Model - MOVE | 1.2.840.10008.5.1.4.1.2.2.2 | — | Yes | — |
| Study Root Query/Retrieve Information Model - GET | 1.2.840.10008.5.1.4.1.2.2.3 | — | — | Yes |

#### 2.1.2 SOP Classes for Storage (SCU)

Weasis can send DICOM objects via C-STORE to a remote DICOM Storage SCP.

| SOP Class Name | SOP Class UID |
|---|---|
| All Storage SOP Classes | See Section 8 |

#### 2.1.3 SOP Classes for Storage (SCP)

Weasis can receive DICOM objects via its built-in DICOM listener (C-STORE SCP).

| SOP Class Name | SOP Class UID |
|---|---|
| All Storage SOP Classes | See Section 8 |

#### 2.1.4 SOP Classes for Print Management (SCU)

| SOP Class Name | SOP Class UID | N-CREATE | N-SET | N-ACTION | N-DELETE |
|---|---|---|---|---|---|
| Basic Grayscale Print Management Meta | 1.2.840.10008.5.1.1.9.1 | Yes | Yes | Yes | Yes |
| Basic Color Print Management Meta | 1.2.840.10008.5.1.1.9.2 | Yes | Yes | Yes | Yes |

#### 2.1.5 SOP Classes for DICOMWeb (Client)

| Service | Description |
|---|---|
| QIDO-RS | Query based on Patient ID, Study Instance UID, Accession Number, Series Instance UID, SOP Instance UID |
| WADO-RS | Retrieve studies, series, instances, frames (multipart/related) |
| WADO-URI | Simple HTTP GET-based retrieval |
| STOW-RS | Store DICOM objects (multipart/related, application/dicom) |

### 2.2 Transfer Options

#### 2.2.2 Transfer Syntaxes (SCU)

Weasis can propose all the following Transfer Syntaxes for presentation in association negotiation:

| Syntax Name | UID | Type |
|---|---|---|
| Implicit VR Little Endian | 1.2.840.10008.1.2 | Default |
| Explicit VR Little Endian | 1.2.840.10008.1.2.1 | Supported |
| Explicit VR Big Endian | 1.2.840.10008.1.2.2 | Supported |
| Deflated Explicit VR Little Endian | 1.2.840.10008.1.2.1.99 | Supported |
| RLE Lossless | 1.2.840.10008.1.2.5 | Supported |
| JPEG Baseline (Process 1) | 1.2.840.10008.1.2.4.50 | Supported |
| JPEG Extended (Process 2 & 4) | 1.2.840.10008.1.2.4.51 | Supported |
| JPEG Lossless (Process 14) | 1.2.840.10008.1.2.4.57 | Supported |
| JPEG Lossless (SV1) | 1.2.840.10008.1.2.4.70 | Supported |
| JPEG-LS Lossless | 1.2.840.10008.1.2.4.80 | Supported |
| JPEG-LS Near-Lossless | 1.2.840.10008.1.2.4.81 | Supported |
| JPEG 2000 Lossless Only | 1.2.840.10008.1.2.4.90 | Supported |
| JPEG 2000 | 1.2.840.10008.1.2.4.91 | Supported |
| JPEG 2000 Part 2 Lossless | 1.2.840.10008.1.2.4.92 | Supported |
| JPEG 2000 Part 2 | 1.2.840.10008.1.2.4.93 | Supported |
| JPEG XL Lossless | 1.2.840.10008.1.2.4.110 | Supported |
| JPEG XL JPEG Recompression | 1.2.840.10008.1.2.4.111 | Supported |
| JPEG XL | 1.2.840.10008.1.2.4.112 | Supported |
| JPIP Referenced | 1.2.840.10008.1.2.4.94 | Supported |
| JPIP Referenced Deflate | 1.2.840.10008.1.2.4.95 | Supported |
| MPEG2 Main Profile/Main Level | 1.2.840.10008.1.2.4.100 | Supported |
| MPEG2 Main Profile/High Level | 1.2.840.10008.1.2.4.101 | Supported |
| MPEG-4 AVC/H.264 HP/Level 4.1 | 1.2.840.10008.1.2.4.102 | Supported |
| MPEG-4 AVC/H.264 BD-compatible HP/Level 4.1 | 1.2.840.10008.1.2.4.103 | Supported |

#### 2.2.3 Transfer Syntax Selection (SCU)

For C-STORE SCU operations, Weasis will store objects using their native Transfer Syntax (as read from the file meta-information). The user can also select a specific Transfer Syntax for transcoding when exporting.

For WADO-RS retrieval, the Accept header supports `multipart/related; type="application/dicom"` with `transfer-syntax=*` to retrieve objects in their original syntax.

### 2.3 Association Initiation Policy

Weasis initiates associations for:
- C-FIND (query)
- C-MOVE/C-GET (retrieve)
- C-STORE (send)
- DICOM Print Management

### 2.4 Association Acceptance Policy

Weasis accepts associations for:
- C-STORE (DICOM listener/receiver)

---

## 3 Network Interfaces

Weasis supports the following network interfaces:

| Interface | Protocol | Port | Configurable |
|---|---|---|---|
| DICOM Query/Retrieve | DICOM (TCP) | Dynamic (SCU) | Yes (calling node) |
| DICOM Storage SCP | DICOM (TCP) | Dynamic (user-defined) | Yes (listener port) |
| DICOM Print | DICOM (TCP) | Dynamic (SCU) | Yes |
| DICOMWeb (QIDO-RS) | HTTP/HTTPS | 80/443 (client) | Yes (URL-based) |
| DICOMWeb (WADO-RS) | HTTP/HTTPS | 80/443 (client) | Yes (URL-based) |
| DICOMWeb (STOW-RS) | HTTP/HTTPS | 80/443 (client) | Yes (URL-based) |
| WADO-URI | HTTP/HTTPS | 80/443 (client) | Yes (URL-based) |

- **Hostname**: Configurable per DICOM node
- **Timeout**: Connect timeout 3000ms, Accept timeout 5000ms (configurable)
- **Encoding**: UTF-8 for DICOMWeb query parameters

---

## 4 Configuration

### 4.1 DICOM Node Configuration

DICOM nodes are configured through the GUI preferences:

- **Calling Nodes** (Local AE): AE Title, Hostname, Port
- **Remote DICOM Nodes**: AE Title, Hostname, Port, Type (Storage/Retrieve)
- **DICOMWeb Nodes**: URL (base), WebType (QIDO-RS, WADO-RS, STOW-RS, WADO-URI, or all combined), HTTP headers, Authentication method

### 4.2 System Properties

| Property | Default | Description |
|---|---|---|
| `weasis.aet` | `WEASIS_AE` | Default AE Title |
| `weasis.dicom.root.uid` | `2.25.` (UUID) | DICOM root UID for UID generation |
| `weasis.import.dicom.qr` | `true` | Enable/disable DICOM Query/Retrieve |
| `weasis.export.dicom.send` | `true` | Enable/disable DICOM Send/STOW-RS |

### 4.3 Cache Configuration

Weasis maintains temporary directories for:
- DICOM export: `{temp}/dicom/`
- Uncompressed cached images: `{cache}/dcm-rawcv/`
- QR session: `{temp}/tmp/qr/`

---

## 5 Support of Extended Character Sets

Weasis supports the following character sets for display and query encoding:

| DICOM Name | Java Encoding | Description |
|---|---|---|
| (none/empty) | US-ASCII | ASCII |
| ISO_IR 100 | ISO-8859-1 | Latin 1 (Western European) |
| ISO_IR 101 | ISO-8859-2 | Latin 2 (Central European) |
| ISO_IR 109 | ISO-8859-3 | Latin 3 (Southern European) |
| ISO_IR 110 | ISO-8859-4 | Latin 4 (Northern European) |
| ISO_IR 144 | ISO-8859-5 | Cyrillic |
| ISO_IR 127 | ISO-8859-6 | Arabic |
| ISO_IR 126 | ISO-8859-7 | Greek |
| ISO_IR 138 | ISO-8859-8 | Hebrew |
| ISO_IR 148 | ISO-8859-9 | Turkish |
| ISO_IR 13 | JIS_X0201 | Japanese Kana |
| ISO_IR 166 | TIS-620 | Thai |
| ISO 2022 IR 87 | x-JIS0208 | Japanese Kanji |
| ISO 2022 IR 159 | JIS_X0212-1990 | Japanese Kanji Supplement |
| ISO 2022 IR 149 | EUC-KR | Korean |
| ISO 2022 IR 58 | GB2312 | Simplified Chinese |
| GB18030 | GB18030 | Chinese (GB18030) |
| ISO_IR 192 | UTF-8 | Default Unicode |

Default character set: **ISO_IR 192 (UTF-8)**.

---

## 6 DICOMWeb Capabilities

### 6.1 QIDO-RS (Query)

Weasis implements the QIDO-RS client for querying DICOM studies.

**Query Parameters Supported**:

| Parameter | Tag |
|---|---|
| Patient ID | (0010,0020) |
| Issuer of Patient ID | (0010,0021) |
| Patient Name | (0010,0010) |
| Accession Number | (0008,0050) |
| Study Instance UID | (0020,000D) |
| Study Description | (0008,1030) |
| Study Date (range) | (0008,0020) |
| Study ID | (0020,0010) |
| Referring Physician Name | (0008,0090) |
| Modalities in Study | (0008,0061) |

**Return Attributes (Study level)**:
(0008,0020), (0008,0030), (0008,0050), (0008,0061), (0008,0090), (0008,1030), (0010,0010), (0010,0020), (0010,0021), (0010,0030), (0010,0040), (0020,000D), (0020,0010)

**Return Attributes (Series level)**:
(0008,0060), (0008,103E), (0020,000E), (0020,0011), (0008,1190)

**Return Attributes (Instance level)**:
(0008,0018), (0020,0013), (0008,1190)

**Query Pattern**:
```
GET {baseURL}/studies?{parameters}&includefield={tags}
GET {baseURL}/studies/{studyUID}/series?includefield={tags}
GET {baseURL}/studies/{studyUID}/series/{seriesUID}/instances?includefield={tags}
```

**Pagination**: Supports `limit` and `offset` parameters.

### 6.2 WADO-RS (Retrieve)

Weasis implements the WADO-RS client for retrieving DICOM instances.

**Request Patterns**:
```
GET {baseURL}/studies/{studyUID}
GET {baseURL}/studies/{studyUID}/series/{seriesUID}
GET {baseURL}/studies/{studyUID}/series/{seriesUID}/instances/{instanceUID}
```

**Accept Header**:
```
Accept: multipart/related; type="application/dicom"; transfer-syntax=*
```

**Image Download**: When Accept is `image/jpeg`, instances are downloaded as JPEG for faster loading.

### 6.3 WADO-URI

Weasis supports WADO-URI for simple HTTP-based retrieval of DICOM objects.

### 6.4 STOW-RS (Store)

Weasis implements the STOW-RS client for uploading DICOM objects.

**Request**:
```
POST {baseURL}/studies
```

**Content-Type**: `multipart/related; type="application/dicom"`

**Response Handling**:
- HTTP 200: All instances stored successfully
- HTTP 202 (Accepted) / HTTP 409 (Conflict): Partial success; failed instances reported in XML response body
- HTTP 401: Token refresh attempted via OAuth2 flow

### 6.5 Authentication

Weasis DICOMWeb client supports:

| Method | Description |
|---|---|
| None | No authentication |
| Basic Auth | HTTP Basic Authentication (Base64-encoded credentials) |
| Bearer Token | OAuth2 / OpenID Connect access tokens |
| Bearer Token (dcm4chee ARC) | Special support for access tokens in URL-encoded header format |

---

## 7 Security Profiles

### 7.1 TLS / Secure Transport

- DICOMWeb connections can use HTTPS (TLS)
- No raw DICOM over TLS (no DICOM TLS profile implemented)

### 7.2 OAuth2 / OpenID Connect

Weasis supports OAuth2 authorization code flow:
- Authorization endpoint
- Token endpoint
- Token refresh (automatic on 401 responses)
- Token revocation endpoint

### 7.3 Anonymization

Weasis includes a configurable anonymization profile that can be enabled to remove Protected Health Information (PHI) on export.

---

## 8 SOP Classes Supported

The following table lists all DICOM SOP Classes that Weasis can read, display, and process. Weasis does not reject any Storage SOP Class; objects stored in unknown SOP Classes are treated as generic DICOM files with metadata display.

### 8.1 Image Storage SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Computed Radiography Image Storage | 1.2.840.10008.5.1.4.1.1.1 | Yes | Yes | No |
| Digital X-Ray Image Storage - For Presentation | 1.2.840.10008.5.1.4.1.1.1.1 | Yes | Yes | No |
| Digital X-Ray Image Storage - For Processing | 1.2.840.10008.5.1.4.1.1.1.1.1 | Yes | Yes | No |
| Digital Mammography X-Ray Image Storage - For Presentation | 1.2.840.10008.5.1.4.1.1.1.2 | Yes | Yes | No |
| Digital Mammography X-Ray Image Storage - For Processing | 1.2.840.10008.5.1.4.1.1.1.2.1 | Yes | Yes | No |
| Digital Intra-Oral X-Ray Image Storage - For Presentation | 1.2.840.10008.5.1.4.1.1.1.3 | Yes | Yes | No |
| Digital Intra-Oral X-Ray Image Storage - For Processing | 1.2.840.10008.5.1.4.1.1.1.3.1 | Yes | Yes | No |
| CT Image Storage | 1.2.840.10008.5.1.4.1.1.2 | Yes | Yes | No |
| Enhanced CT Image Storage | 1.2.840.10008.5.1.4.1.1.2.1 | Yes | Yes | No |
| MR Image Storage | 1.2.840.10008.5.1.4.1.1.4 | Yes | Yes | No |
| Enhanced MR Image Storage | 1.2.840.10008.5.1.4.1.1.4.1 | Yes | Yes | No |
| MR Spectroscopy Storage | 1.2.840.10008.5.1.4.1.1.4.2 | Yes | Yes | No |
| Nuclear Medicine Image Storage | 1.2.840.10008.5.1.4.1.1.20 | Yes | Yes | No |
| Ultrasound Image Storage | 1.2.840.10008.5.1.4.1.1.6.1 | Yes | Yes | No |
| Ultrasound Multi-frame Image Storage | 1.2.840.10008.5.1.4.1.1.3.1 | Yes | Yes | No |
| SC Image Storage | 1.2.840.10008.5.1.4.1.1.6.2 | Yes | Yes | No |
| Multi-frame Single Bit Secondary Capture Image Storage | 1.2.840.10008.5.1.4.1.1.7.1 | Yes | Yes | No |
| Multi-frame Grayscale Byte Secondary Capture Image Storage | 1.2.840.10008.5.1.4.1.1.7.2 | Yes | Yes | No |
| Multi-frame Grayscale Word Secondary Capture Image Storage | 1.2.840.10008.5.1.4.1.1.7.3 | Yes | Yes | No |
| Multi-frame True Color Secondary Capture Image Storage | 1.2.840.10008.5.1.4.1.1.7.4 | Yes | Yes | No |
| X-Ray Angiographic Image Storage | 1.2.840.10008.5.1.4.1.1.12.1 | Yes | Yes | No |
| Enhanced XA Image Storage | 1.2.840.10008.5.1.4.1.1.12.1.1 | Yes | Yes | No |
| X-Ray Radiofluoroscopic Image Storage | 1.2.840.10008.5.1.4.1.1.12.2 | Yes | Yes | No |
| Enhanced XRF Image Storage | 1.2.840.10008.5.1.4.1.1.12.2.1 | Yes | Yes | No |
| X-Ray 3D Angiographic Image Storage | 1.2.840.10008.5.1.4.1.1.13.1.1 | Yes | Yes | No |
| X-Ray 3D Craniofacial Image Storage | 1.2.840.10008.5.1.4.1.1.13.1.2 | Yes | Yes | No |
| Breast Tomosynthesis Image Storage | 1.2.840.10008.5.1.4.1.1.13.1.3 | Yes | Yes | No |
| Breast Projection X-Ray Image Storage - For Presentation | 1.2.840.10008.5.1.4.1.1.13.1.5 | Yes | Yes | No |
| Parametric Map Storage | 1.2.840.10008.5.1.4.1.1.30 | Yes | Yes | No |
| Enhanced PET Image Storage | 1.2.840.10008.5.1.4.1.1.130 | Yes | Yes | No |
| Ophthalmic Tomography Image Storage | 1.2.840.10008.5.1.4.1.1.80.92 | Yes | Yes | No |
| Wide Field Ophthalmic Photography Stereographic Projection Image Storage | 1.2.840.10008.5.1.4.1.1.80.93 | Yes | Yes | No |
| Wide Field Ophthalmic Photography 3D Coordinates Image Storage | 1.2.840.10008.5.1.4.1.1.80.94 | Yes | Yes | No |
| VL Whole Slide Microscopy Image Storage | 1.2.840.10008.5.1.4.1.1.77.1.6 | Yes | Yes | No |
| Dermoscopic Photography Image Storage | 1.2.840.10008.5.1.4.1.1.77.1.7 | Yes | Yes | No |

### 8.2 Video Storage SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Video Endoscopic Image Storage | 1.2.840.10008.5.1.4.1.1.77.1.1.1 | Yes | Yes | No |
| Ultrasound Multi-frame Image Storage (Retired) | 1.2.840.10008.5.1.4.1.1.3 | Yes | Yes | No |
| 12-lead ECG Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.1.1 | Yes | Yes | No |
| General ECG Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.1.2 | Yes | Yes | No |

All video SOP classes using MPEG2, MPEG-4 AVC/H.264 Transfer Syntaxes are supported for playback.

### 8.3 Structured Report SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Basic Text SR | 1.2.840.10008.5.1.4.1.1.88.11 | Yes | Yes | No |
| Enhanced SR | 1.2.840.10008.5.1.4.1.1.88.22 | Yes | Yes | No |
| Comprehensive SR | 1.2.840.10008.5.1.4.1.1.88.33 | Yes | Yes | No |
| Procedure Log | 1.2.840.10008.5.1.4.1.1.88.40 | Yes | Yes | No |
| Mammography CAD SR | 1.2.840.10008.5.1.4.1.1.88.50 | Yes | Yes | No |
| Key Object Selection Document | 1.2.840.10008.5.1.4.1.1.88.59 | Yes | Yes | No |
| Chest CAD SR | 1.2.840.10008.5.1.4.1.1.88.65 | Yes | Yes | No |
| X-Ray Radiation Dose SR | 1.2.840.10008.5.1.4.1.1.88.67 | Yes | Yes | No |

### 8.4 Encapsulated Document SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Encapsulated PDF Storage | 1.2.840.10008.5.1.4.1.1.104.1 | Yes | Yes | No |
| Encapsulated CDA Storage | 1.2.840.10008.5.1.4.1.1.104.2 | Yes | Yes | No |
| Encapsulated STL Storage | 1.2.840.10008.5.1.4.1.1.104.3 | Yes | Yes | No |

### 8.5 Presentation State SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Grayscale Softcopy Presentation State Storage | 1.2.840.10008.5.1.4.1.1.11.1 | Yes | Yes | Yes |
| Color Softcopy Presentation State Storage | 1.2.840.10008.5.1.4.1.1.11.2 | Yes | Yes | No |
| Blending Softcopy Presentation State Storage | 1.2.840.10008.5.1.4.1.1.11.3 | Yes | Yes | No |

### 8.6 Radiotherapy SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| RT Structure Set Storage | 1.2.840.10008.5.1.4.1.1.481.3 | Yes | Yes | No |
| RT Plan Storage | 1.2.840.10008.5.1.4.1.1.481.5 | Yes | Yes | No |
| RT Dose Storage | 1.2.840.10008.5.1.4.1.1.481.2 | Yes | Yes | No |

### 8.7 Segmentation and Surface SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Segmentation Storage | 1.2.840.10008.5.1.4.1.1.66.4 | Yes | Yes | No |
| Surface Segmentation Storage | 1.2.840.10008.5.1.4.1.1.66.5 | Yes | Yes | No |

### 8.8 Waveform SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| 12-lead ECG Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.1.1 | Yes | Yes | No |
| General ECG Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.1.2 | Yes | Yes | No |
| Ambulatory ECG Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.1.3 | Yes | Yes | No |
| Hemodynamic Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.2.1 | Yes | Yes | No |
| Cardiac Electrophysiology Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.3.1 | Yes | Yes | No |
| Basic Voice Audio Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.4.1 | Yes | Yes | No |
| General Audio Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.4.2 | Yes | Yes | No |

### 8.9 Audio SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Basic Voice Audio Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.4.1 | Yes | Playback | No |
| General Audio Waveform Storage | 1.2.840.10008.5.1.4.1.1.9.4.2 | Yes | Playback | No |

### 8.10 Other SOP Classes

| SOP Class Name | SOP Class UID | Read | Display | Write |
|---|---|---|---|---|
| Spatial Fiducials Storage | 1.2.840.10008.5.1.4.1.1.66.1 | Yes | Yes | No |
| Spatial Registration Storage | 1.2.840.10008.5.1.4.1.1.66.2 | Yes | Yes | No |
| Deformable Spatial Registration Storage | 1.2.840.10008.5.1.4.1.1.66.3 | Yes | Yes | No |
| Raw Data Storage | 1.2.840.10008.5.1.4.1.1.66 | Yes | Yes | No |
| Real World Value Mapping Storage | 1.2.840.10008.5.1.4.1.1.67 | Yes | Yes | No |
| Hanging Protocol Storage | 1.2.840.10008.5.1.4.1.1.11.4 | Yes | Yes | No |
| Color Palette Storage | 1.2.840.10008.5.1.4.1.1.11.5 | Yes | Yes | No |

---

## 9 Modality Support

Weasis recognizes and provides modality-specific display configurations for a wide range of modalities. Specialized viewers are available for:

| Modality | Description | Viewer |
|---|---|---|
| PR | Presentation State | Softcopy Presentation State renderer |
| KO | Key Object Selection | Key Object viewer |
| SEG | Segmentation | Segmentation overlay renderer |
| SR | Structured Report | SR document viewer (HTML) |
| RTSTRUCT | RT Structure Set | RT Structure overlay on images |
| RTPLAN | RT Plan | RT Plan viewer |
| RTDOSE | RT Dose | RT Dose overlay + DVH |
| ECG | Electrocardiography | Waveform viewer (12-lead) |
| HD | Hemodynamic Waveform | Waveform viewer |
| AU | Audio | Audio playback |
| DOC | Document | Encapsulated document viewer |

---

## 10 Implementation Notes

### 10.1 DICOM Root UID

Weasis uses UUID-based UIDs (root 2.25.) by default, as recommended by DICOM for generating globally unique identifiers without registration. The root UID can be configured via the `weasis.dicom.root.uid` property.

### 10.2 Codec Architecture

Weasis uses:
- **dcm4che3**: DICOM image reader SPI for parsing DICOM files and network operations
- **OpenCV** (via JavaCPP): Image decompression, pixel data processing, format conversion

### 10.3 WADO Instance Reference

When retrieving via WADO-RS/WADO-URI, Weasis uses the WADO Instance Reference List to track which instances need to be downloaded. Each instance is referenced by:
- SOP Instance UID
- Instance Number
- Retrieve URL (direct download URL for the instance)

### 10.4 C-GET SOP Class

For C-GET operations, Weasis supports a configurable list of SOP Class UIDs to determine which objects to retrieve. This configuration is loaded from a resource file at `CGET_SOP_UID`.

### 10.5 DICOM Listener

The Weasis DICOM listener (Store SCP) stores received objects in a temporary directory and triggers automatic loading into the explorer model as files arrive.

---

## 11 References

| Reference | Description |
|---|---|
| PS3.1 | DICOM Introduction and Overview |
| PS3.2 | DICOM Conformance |
| PS3.3 | DICOM Information Object Definitions |
| PS3.4 | DICOM Service Class Specifications |
| PS3.5 | DICOM Data Structures and Encoding |
| PS3.6 | DICOM Data Dictionary |
| PS3.7 | DICOM Message Exchange |
| PS3.8 | DICOM Network Communication Support for Message Exchange |
| PS3.10 | DICOM Media Storage and File Format for Media Interchange |
| PS3.18 | DICOMWeb Services |
| dcm4che3 | dcm4che3 DICOM Toolkit (https://www.dcm4che.org) |
| OpenCV | Open Source Computer Vision Library (https://opencv.org) |
