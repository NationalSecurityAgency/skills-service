SkillTree allows embedding video or audio clips into skill descriptions, with optional configuration settings to achieve the skill upon full playback.

### Videos
You can either embed externally hosted videos or upload videos directly to SkillTree.

- **SkillTree Hosted**: Upload videos using the file-upload form (Browse button) or by dragging and dropping video files
    - Supported video formats:
        - [WebM](https://www.webmproject.org/about/) - A free video format specifically created for the web
        - [MP4](https://en.wikipedia.org/wiki/MP4_file_format) - A common video format often used in web applications
- **Externally Hosted**: Enter a URL that points to a video hosted on another web server (e.g., `https://example.com/video.mp4`)

#### Configuration
1. Navigate to `Project -> Subject -> Skill -> Audio/Video`
2. The only required field is `Video`
3. Once configured, the video appears above the skill's description on the training page

#### Uploading Videos
- **From your computer**: Click `Browse` or drag and drop a video file
- **Externally hosted**: Click `Switch to External Link` and enter the video URL
- Click `Save and Preview` to apply changes

#### Accessibility
Configure these optional settings for better accessibility:
- **Captions**: Uses [WebVTT format](https://developer.mozilla.org/en-US/docs/Web/API/WebVTT_API)
    - Click `Add Example` for a WebVTT template
- **Transcript**: Provide a text transcript of the video content

> **TIP**: Resize videos by dragging the bottom-right corner. Click **Save Changes** to apply the new dimensions.

> **TIP**: Trainees can resize videos to their preference. These settings are saved in the browser's local storage and override the default dimensions.

### Audio
Users can upload audio files by navigating to `Project -> Subject -> Skill -> Audio/Video` and selecting the desired file using the Browse button or drag-n-drop functionality.

The following audio formats are supported:
- WAV
- MPEG
- MP4
- AAC
- AACP
- OGG
- WEBM
- FLAC

The Audio/Video settings also provide an option to add a transcript, enhancing accessibility for users.

### Achieving Skills through Audio/Video Playback
Once the Audio/Video settings are specified, the skill can be configured to be achieved after the trainee has watched or listened to the entire audio/video clip (>96%). This is done using the Self Reporting mechanism, which can be easily selected by editing the skill and choosing the `Audio/Video` Self Reporting type.

Additionally, if a transcript is configured for the audio/video, users can also earn the skill by reading the transcript and self-certifying to receive credit.