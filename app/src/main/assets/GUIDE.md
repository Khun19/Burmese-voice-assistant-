# ပြီးပြည့်စုံသော အော့ဖ်လိုင်း မြန်မာ Voice Assistant တည်ဆောက်ခြင်း လမ်းညွှန်

ဤလမ်းညွှန်သည် အင်တာနက်မလိုဘဲ အလုပ်လုပ်နိုင်သော မြန်မာဘာသာစကားဖြင့် အမိန့်ပေးနိုင်သည့် Voice Assistant (TFLite နှင့် ONNX အသုံးပြု) တည်ဆောက်ခြင်း အဆင့် (၁၀) ဆင့်ကို အသေးစိတ် ဖော်ပြထားပါသည်။

## Phase 1: Data Preparation & Preprocessing
* **လိုအပ်သော ဒေတာ (Dataset)**: မြန်မာအသံဖိုင်များနှင့် စာသားများ (ဥပမာ - `myanmar-language-dataset-collection`)
* **ကိရိယာများ**: Python, Librosa, Pandas
* **လုပ်ဆောင်ချက်**: 
  - အသံဖိုင်များကို 16kHz, 16-bit Mono WAV format သို့ပြောင်းပါ။
  - Noise reduction နှင့် Silence trimming ပြုလုပ်ပါ။
  - Transcript များကို သန့်စင်ပြီး Tokenization ပြုလုပ်ပါ။

## Phase 2: Core Model Training (STT, NLU, TTS)
* **STT (Speech-to-Text)**: OpenAI Whisper (Tiny/Base) ကို မြန်မာအသံဖြင့် Fine-tune လုပ်ပါ။ (HuggingFace `transformers` အသုံးပြု)
* **NLU (Natural Language Understanding)**: XLM-R သို့မဟုတ် mBART ကို အသုံးပြု၍ Intent Classification နှင့် Slot Filling အတွက် Train ပါ။
* **TTS (Text-to-Speech)**: Coqui TTS (VITS သို့မဟုတ် Tacotron2) ကို မြန်မာအသံ ဒေတာဖြင့် Train ပါ။

## Phase 3: Model Conversion & Optimization (TFLite & ONNX)
* **TFLite (STT)**: 
  ```python
  import tensorflow as tf
  # Convert STT to TFLite
  converter = tf.lite.TFLiteConverter.from_saved_model('stt_model_dir')
  converter.optimizations = [tf.lite.Optimize.DEFAULT]
  tflite_model = converter.convert()
  with open('whisper_mm.tflite', 'wb') as f: f.write(tflite_model)
  ```
* **ONNX (NLU & TTS)**:
  ```python
  import torch
  # Export NLU to ONNX
  torch.onnx.export(nlu_model, dummy_input, "nlu_mm.onnx", opset_version=14)
  ```
* **Quantization**: ဖုန်းပေါ်တွင် လျင်မြန်စွာ အလုပ်လုပ်ရန် Float16 သို့မဟုတ် INT8 Quantization ပြုလုပ်ပါ။

## Phase 4: Android Project Setup & Engine Implementation
* **Dependencies**: `tensorflow-lite`, `onnxruntime-android` ကို `build.gradle.kts` တွင်ထည့်ပါ။
* **Engines**: 
  - `STTEngine`: TFLite Interpreter ကိုသုံး၍ အသံလှိုင်း (PCM) မှ စာသား (Text) သို့ပြောင်းပါ။
  - `NLUEngine`: OrtEnvironment နှင့် OrtSession သုံး၍ စာသားမှ Intent ခွဲခြားပါ။
  - `TTSEngine`: Android ၏ Native `TextToSpeech` (သို့) ပြင်ပ ONNX TTS Model ကို သုံး၍ အသံပြန်ထုတ်ပါ။
  - `WakeWordEngine`: "Hey Bro" သို့မဟုတ် "ဟယ်လို" ကဲ့သို့ စကားလုံးအတွက် Micro KWS (Keyword Spotting) မော်ဒယ်အသေးကို သုံးပါ။

## Phase 5: Privacy & Permissions Management
* **Permissions**: `RECORD_AUDIO` ကို `AndroidManifest.xml` တွင် ကြေငြာပါ။
* **Runtime Request**: App စတင်ချိန်တွင် အသံဖမ်းယူခွင့် (Microphone Permission) တောင်းခံပါ။

## Phase 6: Core Logic Workflow
1. **Wake Word Detection**: Background တွင် အမြဲနားထောင်နေပြီး "Hey Bro" ဟုကြားလျှင် စတင်အလုပ်လုပ်သည်။
2. **Audio Capture**: အသုံးပြုသူ၏ အသံကို ဖမ်းယူပြီး TFLite STT Engine သို့ ပေးပို့သည်။
3. **Intent Recognition**: ရရှိလာသော မြန်မာစာသားကို ONNX NLU Engine က နားလည်ပြီး လုပ်ဆောင်ရမည့်အရာ (ဥပမာ - မီးဖွင့်ရန်၊ သီချင်းဖွင့်ရန်) ကို ခွဲခြားသည်။
4. **Action Execution**: `SystemController` သို့မဟုတ် `DialogueManager` က သတ်မှတ်ထားသော လုပ်ဆောင်ချက်ကို လုပ်ဆောင်သည်။
5. **Voice Feedback**: ပြီးစီးကြောင်းကို မြန်မာလို အသံဖြင့် (TTS) ပြန်လည်ပြောကြားသည်။

## Phase 7: Model Size & Performance Footprint
* **STT Model (Quantized)**: ~40 MB
* **NLU Model (Quantized)**: ~80 MB
* **TTS Model (Quantized/Native)**: ~50 MB (Native သုံးလျှင် 0 MB)
* **RAM Usage**: inference အချိန်တွင် ~200-300 MB အထိ အသုံးပြုနိုင်သည်။

## Phase 8: Optimization & Edge Deployment Tips
* **Audio Buffering**: အသံဖမ်းယူရာတွင် Circular Buffer ကို သုံး၍ Memory ကို ချွေတာပါ။
* **Hardware Acceleration**: NNAPI သို့မဟုတ် XNNPACK ကို ဖွင့်ထားပါ။
* **Asynchronous Execution**: Model Inference ကို Main Thread တွင် မလုပ်ဘဲ Coroutines ဖြင့် Background တွင် သီးသန့်လုပ်ပါ။

## Phase 9: Quick Start Developer Checklist
- [x] Model များကို TFLite/ONNX ပြောင်းပြီး `assets` folder တွင် ထည့်ပြီးပြီလား?
- [x] Microphone Permission ကို Runtime တွင် တောင်းခံထားသလား?
- [x] `SystemController` တွင် System အမိန့်များ (Wifi, Bluetooth) ရေးထားသလား?
- [x] UI တွင် Status နှင့် Chat Log များကို ပြသနိုင်ပြီလား?

## Phase 10: Learning Resources & Links
* [TensorFlow Lite Android Guide](https://www.tensorflow.org/lite/android)
* [ONNX Runtime Android API](https://onnxruntime.ai/docs/execution-providers/NNAPI-ExecutionProvider.html)
* [HuggingFace Transformers](https://huggingface.co/docs/transformers/index)
* [Android AudioRecord Documentation](https://developer.android.com/reference/android/media/AudioRecord)
