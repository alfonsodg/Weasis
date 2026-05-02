# Weasis Software Architecture Document

| Field | Value |
|---|---|
| Document ID | WES-ARCH-001 |
| Version | 1.0 |
| Date | 2026-05-02 |
| Status | DRAFT |
| Classification | Internal |
| Template | IEC 62304 - Software Architecture |

---

## 1. Introduction

### 1.1 Purpose

This document describes the software architecture of Weasis, an open-source DICOM viewer for medical image visualization. It follows the C4 model (Context, Containers, Components) and addresses the architecture requirements of IEC 62304 for medical device software.

### 1.2 Scope

The scope covers the software architecture of Weasis version 4.7.0, including all modules, interfaces, data flows, deployment options, and security mechanisms.

### 1.3 Definitions and Acronyms

| Term | Definition |
|---|---|
| AE | Application Entity (DICOM) |
| DICOM | Digital Imaging and Communications in Medicine |
| DICOMWeb | RESTful DICOM services (QIDO-RS, WADO-RS, STOW-RS) |
| GSPS | Grayscale Softcopy Presentation State |
| IHE | Integrating the Healthcare Enterprise |
| ATNA | Audit Trail and Node Authentication |
| MPR | Multiplanar Reconstruction |
| OSGi | Open Services Gateway Initiative |
| PACS | Picture Archiving and Communication System |
| SCP | Service Class Provider |
| SCU | Service Class User |
| SOP | Service-Object Pair |

### 1.4 References

- IEC 62304:2006 + AMD1:2015 - Medical Device Software - Software Life Cycle Processes
- ISO 14971:2019 - Risk Management for Medical Devices
- DICOM PS3.1-PS3.20 - DICOM Standard
- IHE IT Infrastructure Technical Framework
- WES-RISK-001 - Risk Assessment Document

---

## 2. System Overview

Weasis is a multi-platform desktop DICOM viewer built on the Eclipse Equinox OSGi framework. It uses the dcm4che3 library for DICOM network operations and OpenCV (via JavaCPP) for image decompression and processing. The software operates as a modular, plugin-based architecture where each functional unit is an OSGi bundle.

**Key architectural characteristics:**
- **OSGi modularity**: Eclipse Equinox framework with hot-pluggable bundles
- **Plugin-based**: All modules are OSGi bundles with well-defined service interfaces
- **Cross-platform**: Java 25 runtime on Windows, macOS, Linux (x86-64, ARM64)
- **Dual-licensing**: EPL 2.0 and Apache 2.0

---

## 3. C4 Model Diagrams

### 3.1 Context Diagram (Level 1)

```mermaid
graph TB
    subgraph "Healthcare Enterprise"
        CLINICIAN["Clinician / Radiologist<br/>Person"]
        PACS["PACS System<br/>Software System"]
        DICOMWEB["DICOMWeb Server<br/>Software System"]
        EHR["EHR / RIS / HIS<br/>Software System"]
        DICOM_PRINT["DICOM Print SCP<br/>Software System"]
        LDAP["LDAP / OIDC Provider<br/>Software System"]
    end

    subgraph "Weasis"
        WEASIS["Weasis DICOM Viewer<br/>Desktop Application<br/>Java/OSGi"]
    end

    CLINICIAN -- "Views medical images<br/>Takes measurements<br/>Exports/prints studies" --> WEASIS
    WEASIS -- "C-FIND, C-MOVE, C-GET<br/>C-STORE, DICOMWeb" --> PACS
    WEASIS -- "QIDO-RS, WADO-RS, WADO-URI<br/>STOW-RS" --> DICOMWEB
    WEASIS -- "Weasis Protocol URI<br/>(weasis://)" --> EHR
    WEASIS -- "DICOM Print (SCU)" --> DICOM_PRINT
    WEASIS -- "OAuth2 / OpenID Connect" --> LDAP

    style WEASIS fill:#2962FF,color:#ffffff,stroke:#0D47A1
    style CLINICIAN fill:#388E3C,color:#ffffff,stroke:#1B5E20
    style PACS fill:#F57C00,color:#ffffff,stroke:#E65100
    style DICOMWEB fill:#F57C00,color:#ffffff,stroke:#E65100
    style EHR fill:#F57C00,color:#ffffff,stroke:#E65100
    style DICOM_PRINT fill:#F57C00,color:#ffffff,stroke:#E65100
    style LDAP fill:#F57C00,color:#ffffff,stroke:#E65100
```

### 3.2 Container Diagram (Level 2)

```mermaid
graph TB
    subgraph "Weasis - Container View"
        LAUNCHER["Weasis Launcher<br/>OSGi Framework<br/>Equinox + Felix"]

        subgraph "Core Layer"
            CORE_API["Core API<br/>org.weasis.core.api<br/>Service Interfaces, Image Ops"]
            CORE_UI["Core UI<br/>org.weasis.core.ui<br/>GUI Framework, Docking"]
            CORE_UTIL["Core Util<br/>org.weasis.core.util<br/>Utilities"]
        end

        subgraph "Base Layer"
            BASE_UI["Base UI<br/>org.weasis.base.ui<br/>Window Management"]
            BASE_VIEWER["Base Viewer 2D<br/>org.weasis.base.viewer2d<br/>Image Display"]
            BASE_EXPLORER["Base Explorer<br/>org.weasis.base.explorer<br/>File Navigation"]
        end

        subgraph "DICOM Layer"
            DICOM_CODEC["DICOM Codec<br/>org.weasis.dicom.codec<br/>DICOM I/O"]
            DICOM_EXPLORER["DICOM Explorer<br/>org.weasis.dicom.explorer<br/>Study Browser"]
            DICOM_VIEWER["DICOM Viewer 2D<br/>org.weasis.dicom.viewer2d<br/>DICOM Display"]
            DICOM_SEND["DICOM Send<br/>org.weasis.dicom.send<br/>C-STORE + STOW-RS"]
            DICOM_QR["DICOM Query/Retrieve<br/>org.weasis.dicom.qr<br/>C-FIND/C-MOVE/C-GET"]
            DICOM_SR["DICOM SR<br/>org.weasis.dicom.sr<br/>Structured Reports"]
            DICOM_WAVE["DICOM Waveform<br/>org.weasis.dicom.wave<br/>ECG Viewer"]
            DICOM_RT["DICOM RT<br/>org.weasis.dicom.rt<br/>Radiotherapy Viewer"]
            DICOM_AU["DICOM Audio<br/>org.weasis.dicom.au<br/>Audio Player"]
            DICOM_3D["DICOM 3D<br/>org.weasis.dicom.3d<br/>Volume Rendering"]
            DICOM_ISO["ISO Writer<br/>org.weasis.dicom.isowriter<br/>CD/DVD Burning"]
        end

        subgraph "Acquire Layer"
            ACQUIRE_EXPLORER["Acquire Explorer<br/>org.weasis.acquire.explorer"]
            ACQUIRE_EDITOR["Acquire Editor<br/>org.weasis.acquire.editor"]
        end

        subgraph "Infrastructure"
            OPENCV["OpenCV Bindings<br/>weasis-opencv<br/>Native Image Processing"]
            IMAGEIO["ImageIO Codecs<br/>weasis-imageio<br/>Format Readers/Writers"]
        end
    end

    LAUNCHER --> CORE_API
    LAUNCHER --> CORE_UI
    LAUNCHER --> CORE_UTIL
    CORE_UI --> BASE_UI
    BASE_UI --> BASE_VIEWER
    BASE_UI --> BASE_EXPLORER
    DICOM_CODEC --> CORE_API
    DICOM_CODEC --> OPENCV
    DICOM_CODEC --> IMAGEIO
    DICOM_EXPLORER --> DICOM_CODEC
    DICOM_VIEWER --> DICOM_CODEC
    DICOM_VIEWER --> BASE_VIEWER
    DICOM_VIEWER --> DICOM_SR
    DICOM_SEND --> DICOM_CODEC
    DICOM_QR --> DICOM_CODEC
    DICOM_SR --> DICOM_CODEC
    DICOM_WAVE --> DICOM_CODEC
    DICOM_RT --> DICOM_CODEC
    DICOM_AU --> DICOM_CODEC
    DICOM_3D --> DICOM_CODEC
    DICOM_ISO --> DICOM_CODEC
    ACQUIRE_EXPLORER --> DICOM_CODEC
    ACQUIRE_EDITOR --> ACQUIRE_EXPLORER

    style LAUNCHER fill:#E53935,color:#ffffff,stroke:#B71C1C
    style CORE_API fill:#1E88E5,color:#ffffff,stroke:#1565C0
    style CORE_UI fill:#1E88E5,color:#ffffff,stroke:#1565C0
    style CORE_UTIL fill:#1E88E5,color:#ffffff,stroke:#1565C0
    style BASE_UI fill:#43A047,color:#ffffff,stroke:#2E7D32
    style BASE_VIEWER fill:#43A047,color:#ffffff,stroke:#2E7D32
    style BASE_EXPLORER fill:#43A047,color:#ffffff,stroke:#2E7D32
    style DICOM_CODEC fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_EXPLORER fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_VIEWER fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_SEND fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_QR fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_SR fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_WAVE fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_RT fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_AU fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_3D fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style DICOM_ISO fill:#8E24AA,color:#ffffff,stroke:#4A148C
    style ACQUIRE_EXPLORER fill:#00897B,color:#ffffff,stroke:#004D40
    style ACQUIRE_EDITOR fill:#00897B,color:#ffffff,stroke:#004D40
    style OPENCV fill:#FDD835,color:#000000,stroke:#F9A825
    style IMAGEIO fill:#FDD835,color:#000000,stroke:#F9A825
```

### 3.3 Component Diagram (Level 3) - Core API

```mermaid
graph TB
    subgraph "Core API - org.weasis.core.api"
        SERVICE["Service Layer<br/>AuditEventLogger<br/>AtnaAuditLogger<br/>BundleTools<br/>BundlePreferences<br/>WProperties"]

        IMAGE_OPS["Image Operations<br/>OpManager<br/>WindowOp, ZoomOp<br/>RotationOp, CropOp<br/>PseudoColorOp<br/>AutoLevelsOp"]

        MEDIA["Media Framework<br/>Codec Interface<br/>MediaElement<br/>MediaSeries<br/>Series"]

        NETWORK["Networking<br/>HttpUtils, URIUtils<br/>NetworkUtil<br/>SocketUtil"]

        AUTH["Authentication<br/>AuthProvider, AuthMethod<br/>OAuth2ServiceFactory<br/>OpenIdOAuth2<br/>JavaNetHttpClient"]

        GUI["GUI Framework<br/>Insertable, InsertableFactory<br/>ActionState, ActionW<br/>PreferencesPageFactory<br/>DataExplorerView"]

        UTIL["Utilities<br/>CipherProvider (AES-256-GCM)<br/>GzipManager<br/>ThreadUtil, FontTools<br/>ResourceUtil"]
    end

    SERVICE --> AUDIT["AUDIT Log<br/>(logback)"]
    SERVICE --> ATNA["ATNA XML<br/>(RFC 3881)"]
    IMAGE_OPS --> OPENCV_NATIVE["OpenCV Mat<br/>Operations"]
    AUTH --> OIDC["OpenID Connect<br/>Provider"]
    UTIL --> CIPHER["Java Cryptography<br/>Extension (JCE)"]

    style SERVICE fill:#1E88E5,color:#ffffff
    style IMAGE_OPS fill:#1E88E5,color:#ffffff
    style MEDIA fill:#1E88E5,color:#ffffff
    style NETWORK fill:#1E88E5,color:#ffffff
    style AUTH fill:#1E88E5,color:#ffffff
    style GUI fill:#1E88E5,color:#ffffff
    style UTIL fill:#1E88E5,color:#ffffff
    style AUDIT fill:#FFB300,color:#000000
    style ATNA fill:#FFB300,color:#000000
    style OPENCV_NATIVE fill:#FFB300,color:#000000
    style OIDC fill:#FFB300,color:#000000
    style CIPHER fill:#FFB300,color:#000000
```

### 3.4 Component Diagram (Level 3) - DICOM Explorer

```mermaid
graph TB
    subgraph "DICOM Explorer - org.weasis.dicom.explorer"
        MODEL["DicomModel<br/>(Data Explorer Model)"]
        EXPLORER_UI["DicomExplorer<br/>(Main UI Panel)"]

        subgraph "Data Access"
            IMPORT["Import Pipeline<br/>DicomImport<br/>LocalImport<br/>DicomDirImport<br/>DicomZipMediaIO"]
            EXPORT["Export Pipeline<br/>ExportDicom<br/>LocalExport<br/>DicomExportPR"]
            WADO["WADO Download<br/>DownloadManager<br/>SeriesDownloadManager<br/>LoadRemoteDicomURL"]
            PRINT_SCU["DICOM Print SCU<br/>DicomPrint<br/>DicomPrintDialog"]
        end

        subgraph "Presentation State"
            PR["Presentation State<br/>DicomPrSerializer<br/>DicomExportPR<br/>PrGraphicUtil"]
        end

        subgraph "Preferences"
            NODES["DICOM Node Config<br/>DicomNodeListView<br/>DicomWebNode<br/>DicomPrintNode"]
            AUTH_CFG["Authentication<br/>AuthenticationPersistence<br/>AuthMethodDialog"]
        end
    end

    MODEL --> IMPORT
    MODEL --> WADO
    MODEL --> PR
    EXPLORER_UI --> MODEL
    IMPORT --> EXPORT
    WADO --> IMPORT

    style MODEL fill:#8E24AA,color:#ffffff
    style EXPLORER_UI fill:#8E24AA,color:#ffffff
    style IMPORT fill:#AB47BC,color:#ffffff
    style EXPORT fill:#AB47BC,color:#ffffff
    style WADO fill:#AB47BC,color:#ffffff
    style PRINT_SCU fill:#AB47BC,color:#ffffff
    style PR fill:#CE93D8,color:#000000
    style NODES fill:#CE93D8,color:#000000
    style AUTH_CFG fill:#CE93D8,color:#000000
```

---

## 4. Module Responsibilities

### 4.1 weasis-parent

Maven parent POM providing shared configuration: dependency management, plugin configuration (Spotless, OWASP Dependency Check, CycloneDX SBOM, JaCoCo, SonarCloud), build profiles, and version definitions (Java 25, OSGi 8, dcm4che3, OpenCV 4.13).

### 4.2 weasis-launcher

**Responsibility**: OSGi framework bootstrap and application lifecycle management.

- Initializes Eclipse Equinox / Apache Felix OSGi framework
- Configures Felix start levels, bundle cache, system packages
- Resolves and installs bundles from `base.json`, `dicomizer.json`, or `non-dicom-explorer.json` profiles
- Manages the weasis protocol handler (`weasis://`) for browser-to-application launch
- Provides the `Launcher` main class and `ConfigData` loading

### 4.3 weasis-core

**Responsibility**: Central API bundle providing shared interfaces, utilities, and the GUI docking framework. This is the architectural kernel.

**Key packages and their responsibilities:**

| Package | Responsibility |
|---|---|
| `org.weasis.core.api.gui` | GUI framework contracts: `Insertable`, `InsertableFactory`, `ActionW`, `ActionState`, preferences pages |
| `org.weasis.core.api.gui.util` | UI utilities: `AppProperties`, `GuiExecutor`, `WinUtil`, font management |
| `org.weasis.core.api.image` | Image operation pipeline: `OpManager`, `WindowOp`, `ZoomOp`, `RotationOp`, `CropOp`, `PseudoColorOp`, `AutoLevelsOp`, `FlipOp`, `BrightnessOp`, `FilterOp` |
| `org.weasis.core.api.media.data` | Media model: `MediaElement`, `MediaSeries`, `Codec` interface, `TagW`, `Series`, `FileCache` |
| `org.weasis.core.api.net` | Networking: `HttpUtils`, `URIUtils`, `NetworkUtil`, authenticated HTTP client |
| `org.weasis.core.api.net.auth` | Authentication: `AuthProvider`, `AuthMethod`, `OAuth2ServiceFactory`, `OpenIdOAuth2AccessToken`, `JavaNetHttpClient` |
| `org.weasis.core.api.service` | Service layer: `AuditEventLogger`, `AtnaAuditLogger` (RFC 3881), `AuditLog`, `BundleTools`, `BundlePreferences`, `WProperties` |
| `org.weasis.core.api.util` | Utilities: `CipherProvider` (AES-256-GCM), `GzipManager`, `ThreadUtil` |
| `org.weasis.core.api.explorer` | Data explorer interfaces: `DataExplorerView`, `DataExplorerModel`, `ObservableEvent` |
| `org.weasis.core.ui` | Docking framework, viewer plugin system, `SeriesViewerUI`, `ViewerPluginBuilder`, layout management |
| `org.weasis.core.internal` | Internal Activator, MIME detection, OpenCV codec bridge |

### 4.4 weasis-imageio

**Responsibility**: Java ImageIO codec plugins for non-DICOM image formats including JPEG, PNG, BMP, TIFF, GIF, HDR, PNM, and RAS. Registers custom `ImageReaderSpi` and `ImageWriterSpi` implementations.

### 4.5 weasis-opencv

**Responsibility**: Native OpenCV bindings via JavaCPP for hardware-accelerated image processing.

- Platform-specific native libraries (4.13.0-dcm) for linux-x86-64, linux-aarch64, macosx-x86-64, macosx-aarch64, windows-x86-64
- `PlanarImage` and `Mat` data structures for pixel-level operations
- Used by DICOM codec for decompression, color space conversion, and rendering

### 4.6 weasis-base

#### weasis-base-ui
**Responsibility**: Application window lifecycle. Provides `WeasisWin`, `DesktopAdapter`, main menu, toolbar management, about dialog, licensing display, window listener framework.

#### weasis-base-viewer2d
**Responsibility**: Generic 2D image viewer container without DICOM specifics. Provides `View2d`, `View2dContainer`, `EventManager`, image tools (`ImageTool`), display tools (`DisplayTool`), info layer overlay.

#### weasis-base-explorer
**Responsibility**: Local file system explorer for browsing and opening image files. Provides tree-based file navigation with MIME type detection.

### 4.7 weasis-dicom

#### weasis-dicom-codec
**Responsibility**: DICOM media reading/writing using dcm4che3. Core data types: `DicomMediaIO`, `DicomImageElement`, `DicomSeries`, `DicomVideoElement`, `DicomEncapDocElement`, `DicomSpecialElement`. Handles DICOM tag extraction, pixel data decoding (via OpenCV), image rendering pipeline (Modality LUT, VOI LUT, Presentation LUT), transfer syntax transcoding, and DICOM metadata.

#### weasis-dicom-explorer
**Responsibility**: DICOM study management and user interface.

- `DicomModel` - Central data model (patient/study/series hierarchy)
- `DicomExplorer` - Main study browser panel
- Import pipeline: local files, DICOMDIR, ZIP archives
- Export pipeline: DICOM files, DICOMDIR, ZIP, ISO, TIFF, JPEG, PNG
- WADO download manager for remote study retrieval
- DICOM Print SCU (Basic Grayscale and Color)
- Presentation State (GSPS) import/export
- Hanging protocols configuration

#### weasis-dicom-viewer2d
**Responsibility**: DICOM-specific image viewer extending the base viewer. Adds DICOM toolbars (Modality LUT, Cine, 3D, Key Object, DCM Header), MPR and MIP viewers, info layer with DICOM overlays, presentation state rendering, measurement and annotation overlay.

#### weasis-dicom-sr
**Responsibility**: DICOM Structured Report viewer. Renders SR documents with hyperlinks to referenced images, displays measurement results, and supports SR TID (Template ID) rendering.

#### weasis-dicom-wave
**Responsibility**: DICOM Waveform viewer (ECG). Renders 12-lead and other waveform data, enables measurement cursors, gain/time base scaling, and printing.

#### weasis-dicom-au
**Responsibility**: DICOM Audio viewer. Plays back audio data encapsulated in DICOM AU objects, supports export to WAV format.

#### weasis-dicom-rt
**Responsibility**: Radiotherapy viewer. Displays RT Structure Set contours, RT Dose distributions with DVH (Dose Volume Histogram) charts, and RT Plan isodose lines.

#### weasis-dicom-3d
**Responsibility**: 3D volume rendering and MPR. Uses JOGL (JOGL 2.6.0) for OpenGL acceleration. Provides oblique MPR, Maximum Intensity Projection (MIP), and volume rendering with preset transfer functions.

#### weasis-dicom-send
**Responsibility**: DICOM Storage SCU. Implements C-STORE operation for sending DICOM objects to remote PACS and STOW-RS for DICOMWeb upload. Provides `SendDicomView` UI for selecting studies/series to send.

#### weasis-dicom-qr
**Responsibility**: DICOM Query/Retrieve SCU. Implements C-FIND (study/series/instance level), C-MOVE, C-GET, WADO-URI/WADO-RS retrieval, and QIDO-RS (DICOMWeb query). Provides `DicomQrView` for searching and retrieving studies from remote DICOM nodes.

#### weasis-dicom-isowriter
**Responsibility**: ISO image writer for creating DICOM CD/DVD images. Generates DICOMDIR files with proper Media Storage SOP Classes, and creates bootable ISO image files containing the Weasis portable viewer.

### 4.8 weasis-acquire

#### weasis-acquire-explorer
**Responsibility**: Dicomizer module for converting standard images into DICOM format. Manages acquisition sessions, metadata entry, and DICOM encapsulation workflow.

#### weasis-acquire-editor
**Responsibility**: Image editing tools for acquired images before DICOM conversion. Cropping, rotation, brightness/contrast adjustments, and annotation insertion.

### 4.9 tests

**Responsibility**: Aggregated module for running unit and integration tests across all modules. Uses JUnit 6, Mockito, and JaCoCo for coverage aggregation. Configured via `tests/pom.xml`.

---

## 5. Data Flow Architecture

### 5.1 Primary Data Flow: Import -> Store -> Display -> Measure -> Export

```mermaid
flowchart TB
    subgraph "IMPORT"
        A1["DICOM Local File<br/>(Part 10)"]
        A2["DICOMDIR<br/>(CD/DVD)"]
        A3["DICOM ZIP<br/>(.zip)"]
        A4["DICOMWeb<br/>(WADO-RS/URI)"]
        A5["DICOM Network<br/>(C-GET / C-MOVE)"]
        A6["Standard Images<br/>(JPEG, TIFF, PNG)"]
    end

    subgraph "IMPORT PIPELINE"
        B1["File Detection<br/>MimeInspector"]
        B2["DicomMediaIO<br/>DICOM Parser<br/>dcm4che3"]
        B3["Pixel Data<br/>Decompression<br/>OpenCV / JavaCPP"]
        B4[("Cache Storage<br/>(AppProperties<br/>CACHE_DIR)")]
    end

    subgraph "DISPLAY PIPELINE"
        C1["Modality LUT<br/>Rescale Slope/Intercept"]
        C2["VOI LUT<br/>Window Width/Center"]
        C3["Presentation LUT"]
        C4["PseudoColor<br/>LUT (if color)"]
        C5[("Rendered Image<br/>PlanarImage/Mat")]
        C6["Overlays & Graphics<br/>InfoLayer, Annotations<br/>GSPS Graphics"]
    end

    subgraph "MEASUREMENT"
        D1["Spatial Calibration<br/>PixelSpacing, ImagerPixelSpacing"]
        D2["Measurement Tools<br/>Length, Area, Angle<br/>SUV, Statistics"]
        D3[("Measurement Data<br/>XML / DICOM SR / PR")]
    end

    subgraph "EXPORT"
        E1["DICOM Files<br/>C-STORE / STOW-RS"]
        E2["DICOMDIR + ZIP + ISO"]
        E3["Rendered Image<br/>TIFF, JPEG, PNG"]
        E4["Presentation State<br/>(GSPS)"]
        E5["DICOM Print SCP"]
    end

    A1 --> B1
    A2 --> B1
    A3 --> B1
    A4 --> B1
    A5 --> B1
    A6 --> B1
    B1 --> B2
    B2 --> B3
    B3 --> B4
    B4 --> C1
    C1 --> C2
    C2 --> C3
    C3 --> C4
    C4 --> C5
    C5 --> C6
    C5 --> D1
    D1 --> D2
    D2 --> D3
    C5 --> E3
    B4 --> E1
    B4 --> E2
    D3 --> E4
    D3 --> E5

    style A1 fill:#E3F2FD
    style A2 fill:#E3F2FD
    style A3 fill:#E3F2FD
    style A4 fill:#E3F2FD
    style A5 fill:#E3F2FD
    style A6 fill:#E3F2FD
    style B1 fill:#BBDEFB
    style B2 fill:#BBDEFB
    style B3 fill:#BBDEFB
    style B4 fill:#90CAF9,stroke:#1565C0
    style C1 fill:#C8E6C9
    style C2 fill:#C8E6C9
    style C3 fill:#C8E6C9
    style C4 fill:#C8E6C9
    style C5 fill:#81C784,stroke:#2E7D32
    style C6 fill:#C8E6C9
    style D1 fill:#FFF9C4
    style D2 fill:#FFF9C4
    style D3 fill:#FFF176,stroke:#F9A825
    style E1 fill:#FFCDD2
    style E2 fill:#FFCDD2
    style E3 fill:#FFCDD2
    style E4 fill:#FFCDD2
    style E5 fill:#FFCDD2
```

### 5.2 DICOM Network Data Flow

```mermaid
sequenceDiagram
    participant User as Clinician
    participant UI as DICOM Explorer
    participant NQ as DICOM QR
    participant NS as DICOM Send
    participant DC as DICOM Codec
    participant PACS as Remote PACS

    User->>UI: Open DICOM Explorer
    UI->>UI: Load local studies from cache

    User->>NQ: Search Studies (C-FIND)
    NQ->>PACS: DICOM C-FIND RQ
    PACS-->>NQ: C-FIND Response (Study List)
    NQ-->>UI: Display Results

    User->>NQ: Retrieve Study (C-MOVE / C-GET)
    NQ->>PACS: DICOM C-MOVE RQ
    PACS->>NS: C-STORE (Instance Data)
    NS->>DC: Decode & Cache
    DC-->>NS: Decoded Pixel Data
    NS-->>PACS: C-STORE Response
    PACS-->>NQ: C-MOVE Response

    UI->>DC: Load Series for Display
    DC->>DC: Apply Modality LUT
    DC->>DC: Apply VOI LUT
    DC-->>UI: Rendered Image

    User->>UI: Take Measurements
    UI->>UI: Compute calibrated values
    User->>NS: Send to PACS
    NS->>PACS: C-STORE / STOW-RS
    PACS-->>NS: Storage Commitment
```

---

## 6. Interface Architecture

### 6.1 DICOM Network Interface (dcm4che3)

**Protocol**: DICOM over TCP/IP (port 11112 default or configurable)

**SCU Roles**:
- Storage SCU (C-STORE)
- Query/Retrieve SCU (C-FIND, C-MOVE, C-GET)
- Print Management SCU (Basic Grayscale Print, Basic Color Print)

**SCP Role**:
- Storage SCP (configurable DICOM listener)

**Presentation Contexts**: Supports all standard Storage SOP Classes, Query/Retrieve Information Models (Patient Root, Study Root), Print Management SOP Classes.

**Transfer Syntaxes**: Implicit VR Little Endian, Explicit VR Little Endian, Explicit VR Big Endian, JPEG Baseline, JPEG Extended, JPEG Lossless, JPEG-LS, JPEG 2000, RLE, MPEG2, MPEG4.

**Implementation**: dcm4che3 library version 5.34.2, configured via `DicomManager`.

### 6.2 DICOMWeb Interface

**Protocol**: HTTPS (RESTful)

**Services**:
| Service | Operation | Endpoint Pattern |
|---|---|---|
| QIDO-RS | Query | `{base}/rs/studies?{params}` |
| WADO-RS | Retrieve | `{base}/rs/studies/{uid}/series/{uid}/instances/{uid}` |
| WADO-URI | Retrieve | `{base}/wado?requestType=WADO&studyUID=...` |
| STOW-RS | Store | `{base}/rs/studies/{uid}` |

**Authentication**: Supports Basic Auth, OAuth2/OpenID Connect token-based auth via the `AuthProvider` framework.

### 6.3 IHE Integration Profiles

| Profile | Actor | Role |
|---|---|---|
| ATNA | Secure Application | RFC 3881 audit event generation via `AtnaAuditLogger` |
| SWF.b | Image Display Consumer | Display DICOM images with GSPS |
| PDI | Portable Media Creator | ISO image creation with DICOMDIR |

### 6.4 Local File System Interface

**Read paths**:
- DICOM Part 10 files: Read via `DicomMediaIO` using `DicomFileInputStream`
- DICOMDIR: Parsed via `DicomDirLoader` with dcm4che3 `DicomDirReader`
- ZIP archives: Extracted via `DicomZipCodec` (zip4j 2.11.5)
- Non-DICOM images: Read via Java ImageIO with weasis-imageio codecs

**Write paths**:
- DICOM export: Written via `DicomOutputStream`
- DICOMDIR: Generated via `DicomDirBuilder`
- ISO images: Created via `IsoWriter` with CD/DVD UDF format
- Rendered images: TIFF/JPEG/PNG format via Java ImageIO

**Cache structure**:
```
~/.weasis/
  cache/           # Bundle cache
  dicom/           # DICOM export temp directory
  dcm-rawcv/       # Uncompressed DICOM pixel data (OpenCV)
  log/             # Application logs (rolling file)
  audit-{user}.log # Audit trail log
  pref/            # Bundle preferences
```

### 6.5 Weasis Protocol Interface

**Protocol scheme**: `weasis://`

**Format**: `weasis://{command}/{params}`

**Commands**: `DICOM` (load studies), `IMPORT` (import file), `VIEWER` (open viewer with presets)

**Implementation**: Processed by the launcher, which parses the URI and dispatches to appropriate bundles. The protocol can be registered with the OS as a URI scheme handler for browser-based launch.

---

## 7. Security Architecture

### 7.1 Encryption - CipherProvider

**Class**: `org.weasis.core.api.util.CipherProvider`

**Algorithm**: AES-256-GCM

**Characteristics**:
- Key size: 256 bits
- Mode: GCM (Galois/Counter Mode) - authenticated encryption
- IV: 12 bytes (random, prepended to ciphertext)
- Tag length: 128 bits
- Key generation: `SecureRandom` via `KeyGenerator`
- Key serialization: Base64 encoded/decoded

**Usage**: Protecting DICOM data at rest (encrypted local cache). Referenced by issue #21.

**Implementation** (`weasis-core/src/main/java/.../CipherProvider.java:46`):
```java
public static OutputStream encryptStream(OutputStream outputStream, SecretKey key)
public static InputStream decryptStream(InputStream inputStream, SecretKey key)
```

### 7.2 Audit Trail - AuditEventLogger & AtnaAuditLogger

**Classes**:
- `org.weasis.core.api.service.AuditEventLogger`
- `org.weasis.core.api.service.AtnaAuditLogger`
- `org.weasis.core.api.service.AuditLog`

**Capabilities**:
- Structured audit events for HIPAA / IHE ATNA compliance
- Events: study view, export, print, import, delete, login, logout, configure, launch
- Log injection prevention via `sanitize()` method (control character removal)
- Rolling audit log files: 10 files of 20MB each
- RFC 3881 XML format for ATNA messages
- Outcome indicators: SUCCESS (0), MINOR_FAILURE (4), MAJOR_FAILURE (12)

**Code references**:
- `AuditEventLogger.logStudyAccess()` at `weasis-core/.../AuditEventLogger.java:78`
- `AtnaAuditLogger.format()` at `weasis-core/.../AtnaAuditLogger.java:450`
- `AuditLog.getAuditProperties()` at `weasis-core/.../AuditLog.java:69`

### 7.3 Authentication & Authorization

**Framework**: `org.weasis.core.api.net.auth`

**Supported methods**:
- `DefaultAuthMethod` - Basic HTTP authentication
- `OAuth2ServiceFactory` - OAuth2 authorization code flow
- `OpenIdOAuth2AccessToken` - OpenID Connect token handling

**Components**:
- `AuthProvider` - Service interface for pluggable authentication
- `AuthMethod` - Abstract authentication method with credential storage
- `JavaNetHttpClient` - Authenticated HTTP client with token refresh
- `AsyncCallbackServerHandler` - Local HTTP server for OAuth2 callback

### 7.4 Image Operation Security

All image operations in `org.weasis.core.api.image` implement bounds checking and input validation:
- `WindowOp`: Window width/center clamped to valid range
- `ZoomOp`: Scale factors bounded to prevent resource exhaustion
- `RotationOp`: Rotation angles normalized to 0-360 degrees
- `CropOp`: Crop regions validated against image dimensions

### 7.5 Supply Chain Security

- **OWASP Dependency Check**: Plugin `dependency-check-maven` 12.1.0 with CVSS threshold 7.0 (`failBuildOnCVSS>7`), suppression file `owasp-suppressions.xml`. Referenced by issue #36.
- **CycloneDX SBOM**: `cyclonedx-maven-plugin` 2.9.1 generates aggregate BOM on package phase.
- **Spotless**: `spotless-maven-plugin` 3.2.0 with Google Java Format 1.37.0 for consistent code quality.
- **SonarCloud**: Static analysis with security, reliability, and maintainability ratings.
- **JaCoCo**: Code coverage aggregated across all modules.
- **JAR Signing**: `maven-jarsigner-plugin` 3.1.0 for code signing.

---

## 8. Deployment View

### 8.1 Desktop Deployments

```mermaid
graph TB
    subgraph "Build Pipeline"
        BUILD["Maven Build<br/>JDK 25 + Maven 3.8+"]
        SPOTLESS["Spotless Check<br/>Google Java Format"]
        OWASP["OWASP Dependency Check<br/>CVSS > 7 fails build"]
        CYCLONE["CycloneDX SBOM<br/>BOM Generation"]
        JACOCO["JaCoCo Coverage<br/>Aggregate Report"]
        SONAR["SonarCloud Analysis"]
        JARSIGN["JAR Signing"]
    end

    subgraph "Distribution Packaging"
        WIN["Windows<br/>EXE + MSI<br/>JLink Bundled JRE"]
        MAC["macOS<br/>DMG + APP<br/>x86-64 + ARM64"]
        LINUX["Linux<br/>DEB + RPM + TAR<br/>x86-64 + ARM64"]
        DOCKER["Docker<br/>Build Container<br/>Ubuntu 24.04"]
    end

    subgraph "Deployment Targets"
        DESKTOP["Desktop<br/>End User Station"]
        WEB_START["Web Launch<br/>weasis:// protocol"]
        PORTABLE["Portable Media<br/>CD/DVD/USB"]
    end

    BUILD --> SPOTLESS
    SPOTLESS --> OWASP
    OWASP --> CYCLONE
    CYCLONE --> JACOCO
    JACOCO --> SONAR
    SONAR --> JARSIGN
    JARSIGN --> WIN
    JARSIGN --> MAC
    JARSIGN --> LINUX
    JARSIGN --> DOCKER

    WIN --> DESKTOP
    MAC --> DESKTOP
    LINUX --> DESKTOP
    DESKTOP --> WEB_START
    DESKTOP --> PORTABLE

    style BUILD fill:#E53935,color:#ffffff
    style SPOTLESS fill:#FF9800
    style OWASP fill:#FF9800
    style CYCLONE fill:#FF9800
    style JACOCO fill:#FF9800
    style SONAR fill:#FF9800
    style JARSIGN fill:#FF9800
    style WIN fill:#2196F3,color:#ffffff
    style MAC fill:#2196F3,color:#ffffff
    style LINUX fill:#2196F3,color:#ffffff
    style DOCKER fill:#2196F3,color:#ffffff
    style DESKTOP fill:#4CAF50,color:#ffffff
    style WEB_START fill:#4CAF50,color:#ffffff
    style PORTABLE fill:#4CAF50,color:#ffffff
```

### 8.2 System Requirements

| Component | Minimum | Recommended |
|---|---|---|
| OS | Windows 10+, macOS 12+, Linux (x86-64/ARM64) | Latest OS |
| Java Runtime | JRE 21 (bundled in distributions) | JRE 25 |
| RAM | 2 GB | 8 GB |
| Disk Space | 500 MB (application) | 10 GB (cache) |
| Display | 1280x1024, 24-bit color | 1920x1080+ calibrated |
| GPU | OpenGL 3.3+ (for 3D) | Dedicated GPU |
| Network | DICOM connectivity to PACS | 100 Mbps+ |

---

## 9. Module Dependency Graph

```mermaid
graph BT
    LAUNCHER["weasis-launcher"]
    CORE["weasis-core"]
    OPENCV["weasis-opencv"]
    IMAGEIO["weasis-imageio"]
    BASE_UI["weasis-base-ui"]
    BASE_VIEW["weasis-base-viewer2d"]
    BASE_EXPL["weasis-base-explorer"]
    DCM_CODEC["weasis-dicom-codec"]
    DCM_EXPL["weasis-dicom-explorer"]
    DCM_VIEW["weasis-dicom-viewer2d"]
    DCM_SEND["weasis-dicom-send"]
    DCM_QR["weasis-dicom-qr"]
    DCM_SR["weasis-dicom-sr"]
    DCM_WAVE["weasis-dicom-wave"]
    DCM_AU["weasis-dicom-au"]
    DCM_RT["weasis-dicom-rt"]
    DCM_3D["weasis-dicom-3d"]
    DCM_ISO["weasis-dicom-isowriter"]
    ACQ_EXPL["weasis-acquire-explorer"]
    ACQ_EDIT["weasis-acquire-editor"]

    CORE --> LAUNCHER
    IMAGEIO --> CORE
    OPENCV --> CORE
    BASE_UI --> CORE
    BASE_VIEW --> BASE_UI
    BASE_EXPL --> BASE_UI
    DCM_CODEC --> CORE
    DCM_CODEC --> OPENCV
    DCM_CODEC --> IMAGEIO
    DCM_EXPL --> DCM_CODEC
    DCM_VIEW --> DCM_CODEC
    DCM_VIEW --> BASE_VIEW
    DCM_SEND --> DCM_CODEC
    DCM_QR --> DCM_CODEC
    DCM_SR --> DCM_CODEC
    DCM_WAVE --> DCM_CODEC
    DCM_AU --> DCM_CODEC
    DCM_RT --> DCM_CODEC
    DCM_3D --> DCM_CODEC
    DCM_ISO --> DCM_CODEC
    ACQ_EXPL --> DCM_CODEC
    ACQ_EDIT --> ACQ_EXPL

    style LAUNCHER fill:#E53935,color:#ffffff
    style CORE fill:#1E88E5,color:#ffffff
    style OPENCV fill:#FDD835,color:#000000
    style IMAGEIO fill:#FDD835,color:#000000
```

---

## 10. Software Unit Implementation

### 10.1 Technology Stack

| Component | Technology | Version |
|---|---|---|
| Programming Language | Java | 25 |
| Build System | Maven | 3.8+ |
| OSGi Framework | Eclipse Equinox / Apache Felix | 7.0.5 |
| DICOM Toolkit | dcm4che3 | 5.34.2 |
| Image Processing | OpenCV (via JavaCPP) | 4.13.0 |
| 3D Rendering | JOGL (JogAmp) | 2.6.0 |
| UI Components | FlatLaf, MigLayout, Docking Frames | 3.7.1, 11.4.2, 1.1.7 |
| Logging | SLF4J + Logback | 2.0.17, 1.5.25 |
| JSON Processing | Jackson | 2.18.3 |
| XML Binding | JAXB | 4.0.3 |
| OAuth | ScribeJava | 8.3.3 |
| Charts | XChart | 3.8.8 |
| Testing | JUnit, Mockito | 6.0, 5.21 |

### 10.2 OSGi Bundle Configuration

Each module is packaged as an OSGi bundle using `bnd-maven-plugin` 7.2.1 with the following standard configuration:

```xml
<configuration>
  <bnd>
    -noextraheaders: true
    Export-Package: org.weasis.*
    Import-Package: org.slf4j;version="[2.0,3)",*
  </bnd>
</configuration>
```

Bundle symbolic names follow the pattern `org.weasis.{layer}.{module}`.

### 10.3 Build Reproducibility

The build is configured for reproducible builds:
- `project.build.outputTimestamp=1` in parent POM
- Flatten Maven Plugin for CI-friendly versions
- JAR signing via `maven-jarsigner-plugin`

---

## 11. Error Handling Strategy

| Layer | Strategy | Implementation |
|---|---|---|
| Network | Retry with exponential backoff | `DownloadManager`, `HttpUtils` |
| DICOM | Status code handling per DIMSE | dcm4che3 `Status` class |
| Image Decode | Graceful degradation | `DicomMediaIO` fallback codecs |
| File I/O | Stream closure in finally/try-with-resources | Throughout codebase |
| Security | Fail-secure with logged exception | `CipherProvider`, `AuditEventLogger` |
| GUI | SwingWorker for background tasks | `GuiExecutor`, `DicomTaskManager` |
| OSGi | Service tracking with null checks | `BundleTools`, `BundlePreferences` |

---

## 12. Internationalization

Weasis supports multi-language localization using standard Java `ResourceBundle` properties files:

- `Messages.properties` (default: English)
- `Messages_{locale}.properties` for each supported locale
- Messages files embed annotations for translators: `Alt+A`, `Alt+A, Alt+S`
- Located under `src/main/java/**/messages*.properties` (configured in POM resources)

---

## 13. Configuration Management

### 13.1 Server-Side Configuration

Configuration is managed through the `BundlePreferences` service, which retrieves properties from:
1. Default preferences bundled with the application
2. Server-provided configuration (preferences service URL)
3. User overrides (local preferences)

### 13.2 DICOM Node Configuration

DICOM network destinations are configured through the preference UI:
- DICOM nodes (AE Title, hostname, port)
- DICOMWeb nodes (base URL, authentication)
- DICOM Print nodes (medium type, film size, resolution)

Stored via `WProperties` serialization with optional AES-256-GCM encryption for credentials.

---

## 14. Monitoring & Diagnostics

- **Audit Log**: `audit-{user}.log` with rolling policy
- **Application Log**: Configured via Logback with configurable level, pattern, file rotation
- **Performance Marker**: `*PERF*` marker for performance tracing
- **SonarCloud**: Continuous quality monitoring
- **Memory**: Soft references for image cache (`SoftHashMap`) to prevent OOM

---

## 15. Revisions

| Version | Date | Author | Description |
|---|---|---|---|
| 1.0 | 2026-05-02 | Weasis Team | Initial architecture document |
