"""Composes and synthesizes Howanya Life's background music and sound effects.

Everything here is original and generated from code, so there are no licensing concerns.
Run from the project root:

    python3 tools/HowanyaSounds.py

Writes 16-bit mono WAV files to shared/src/main/res/raw/. The BGM files are exact
loops (note tails that run past the end are wrapped to the start), so they can be
played with MediaPlayer.isLooping without a gap.
"""

import math
import os
import random
import struct
import wave

RATE = 22050
OUT_DIR = os.path.join("shared", "src", "main", "res", "raw")

NOTE_INDEX = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def freq(note):
    """'C5', 'F#4', 'Bb3' -> Hz."""
    name, octave = note[:-1], int(note[-1])
    semitone = NOTE_INDEX[name[0]] + (1 if "#" in name else 0) - (1 if "b" in name[1:] else 0)
    midi = 12 * (octave + 1) + semitone
    return 440.0 * 2 ** ((midi - 69) / 12)


class Track:
    """A sample buffer. In loop mode, sound written past the end wraps to the start."""

    def __init__(self, seconds, loop):
        self.samples = [0.0] * int(seconds * RATE)
        self.loop = loop

    def add(self, start_seconds, sound, gain=1.0):
        start = int(start_seconds * RATE)
        n = len(self.samples)
        for i, s in enumerate(sound):
            j = start + i
            if j >= n:
                if not self.loop:
                    break
                j %= n
            self.samples[j] += s * gain

    def write(self, name, peak=0.7):
        top = max(abs(s) for s in self.samples) or 1.0
        scale = peak / top
        path = os.path.join(OUT_DIR, name)
        with wave.open(path, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(b"".join(
                struct.pack("<h", int(max(-1.0, min(1.0, s * scale)) * 32767)) for s in self.samples
            ))
        print(f"wrote {path} ({len(self.samples) / RATE:.1f}s)")


def envelope(n, attack=0.01, decay=0.08, sustain=0.6, release=0.06):
    a, d, r = int(attack * RATE), int(decay * RATE), int(release * RATE)
    env = []
    for i in range(n):
        if i < a:
            v = i / max(a, 1)
        elif i < a + d:
            v = 1 - (1 - sustain) * (i - a) / max(d, 1)
        else:
            v = sustain
        if i > n - r:
            v *= max(0.0, (n - i) / max(r, 1))
        env.append(v)
    return env


# ------------------------------------------------------------------ instruments

def square(f, seconds, duty=0.25):
    """Soft chiptune lead: pulse wave with a little vibrato."""
    n = int(seconds * RATE)
    env = envelope(n, sustain=0.55)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        phase += f * (1 + 0.004 * math.sin(2 * math.pi * 5.5 * t)) / RATE
        out.append((1.0 if (phase % 1.0) < duty else -1.0) * env[i])
    return smooth(out, 3)


def triangle(f, seconds):
    n = int(seconds * RATE)
    env = envelope(n, attack=0.005, decay=0.05, sustain=0.8, release=0.03)
    return [(2 * abs(2 * ((f * i / RATE) % 1.0) - 1) - 1) * env[i] for i in range(n)]


def bell(f, seconds):
    """Music box: a few inharmonic partials with an exponential decay."""
    n = int(seconds * RATE)
    out = []
    for i in range(n):
        t = i / RATE
        decay = math.exp(-3.2 * t)
        s = (math.sin(2 * math.pi * f * t)
             + 0.35 * math.sin(2 * math.pi * f * 2.0 * t) * math.exp(-6 * t)
             + 0.15 * math.sin(2 * math.pi * f * 3.01 * t) * math.exp(-9 * t))
        attack = min(1.0, t / 0.004)
        out.append(s * decay * attack)
    return out


def kick(seconds=0.18):
    n = int(seconds * RATE)
    out, phase = [], 0.0
    for i in range(n):
        t = i / RATE
        phase += (55 + 110 * math.exp(-t * 30)) / RATE
        out.append(math.sin(2 * math.pi * phase) * math.exp(-t * 18))
    return out


def hat(seconds=0.05, seed=0):
    rnd = random.Random(seed)
    n = int(seconds * RATE)
    prev, out = 0.0, []
    for i in range(n):
        x = rnd.uniform(-1, 1)
        out.append((x - prev) * math.exp(-i / RATE * 70))  # crude high-pass
        prev = x
    return out


def smooth(samples, width):
    """Moving average, to take the harsh edge off raw square waves."""
    out, acc = [], 0.0
    for i, s in enumerate(samples):
        acc += s
        if i >= width:
            acc -= samples[i - width]
        out.append(acc / width)
    return out


# ------------------------------------------------------------------ music

def sequence(track, notes, start, beat, instrument, gain, gate=0.9):
    """notes: list of (note or 'r', length in eighth notes)."""
    t = start
    for note, eighths in notes:
        length = eighths * beat / 2
        if note != "r":
            track.add(t, instrument(freq(note), length * gate), gain)
        t += length
    return t


CHORD_TONES = {
    "C": ["C", "E", "G"], "Am": ["A", "C", "E"], "F": ["F", "A", "C"],
    "G": ["G", "B", "D"], "Em": ["E", "G", "B"],
}


def day_bgm():
    """Bright, bouncy loop for daytime: 16 bars at 112 bpm in C major."""
    bpm = 112
    beat = 60 / bpm
    bar = beat * 4
    chords = ["C", "Am", "F", "G", "C", "Am", "F", "C",
              "F", "G", "Em", "Am", "F", "G", "C", "G"]
    melody = [
        [("E5", 2), ("G5", 2), ("C6", 2), ("G5", 2)],
        [("A5", 3), ("G5", 1), ("E5", 2), ("C5", 2)],
        [("F5", 2), ("A5", 2), ("C6", 2), ("A5", 2)],
        [("G5", 4), ("r", 2), ("D5", 1), ("E5", 1)],
        [("E5", 2), ("G5", 2), ("C6", 2), ("E6", 2)],
        [("D6", 2), ("C6", 2), ("A5", 2), ("E5", 2)],
        [("F5", 2), ("A5", 2), ("G5", 2), ("B5", 2)],
        [("C6", 4), ("r", 4)],
        [("A5", 2), ("C6", 2), ("A5", 2), ("F5", 2)],
        [("B5", 2), ("D6", 2), ("B5", 2), ("G5", 2)],
        [("G5", 3), ("E5", 1), ("B5", 2), ("G5", 2)],
        [("A5", 2), ("C6", 2), ("E6", 2), ("C6", 2)],
        [("F5", 2), ("E5", 2), ("D5", 2), ("C5", 2)],
        [("D5", 3), ("C5", 1), ("B4", 2), ("G4", 2)],
        [("C5", 2), ("E5", 2), ("G5", 2), ("E5", 2)],
        [("D5", 4), ("B4", 2), ("G4", 2)],
    ]
    track = Track(bar * len(chords), loop=True)
    for i, (chord, notes) in enumerate(zip(chords, melody)):
        start = i * bar
        sequence(track, notes, start, beat, square, 0.26)
        root, _, fifth = CHORD_TONES[chord]
        bass = [(root + "3", 2), (fifth + "3", 2), (root + "3", 2), (fifth + "3", 2)]
        sequence(track, bass, start, beat, triangle, 0.32, gate=0.8)
        for b in range(4):
            if b % 2 == 0:
                track.add(start + b * beat, kick(), 0.35)
            track.add(start + b * beat + beat / 2, hat(seed=i * 4 + b), 0.08)
    track.write("bgm_day.wav")


def night_bgm():
    """Gentle music-box lullaby for night: 8 bars at 72 bpm."""
    bpm = 72
    beat = 60 / bpm
    bar = beat * 4
    chords = ["C", "Am", "F", "G", "C", "Em", "F", "G"]
    melody = [
        [("E5", 4), ("G5", 4)],
        [("A5", 4), ("E5", 4)],
        [("F5", 4), ("A5", 2), ("G5", 2)],
        [("G5", 8)],
        [("E5", 4), ("C6", 4)],
        [("B5", 4), ("G5", 4)],
        [("A5", 4), ("F5", 2), ("D5", 2)],
        [("G5", 6), ("r", 2)],
    ]
    track = Track(bar * len(chords), loop=True)
    for i, (chord, notes) in enumerate(zip(chords, melody)):
        start = i * bar
        sequence(track, notes, start, beat, bell, 0.5, gate=1.6)
        tones = CHORD_TONES[chord]
        arpeggio = [tones[0] + "4", tones[1] + "4", tones[2] + "4", tones[0] + "5",
                    tones[2] + "4", tones[1] + "4", tones[2] + "4", tones[1] + "4"]
        for k, note in enumerate(arpeggio):
            track.add(start + k * beat / 2, bell(freq(note), beat * 1.5), 0.18)
        track.add(start, bell(freq(tones[0] + "3"), bar), 0.22)
    track.write("bgm_night.wav", peak=0.6)


# ------------------------------------------------------------------ sound effects

def meow(contour, seconds, vibrato=0.0, brightness=(900, 1500), gain=1.0):
    """A cartoon "nya": a harmonic tone whose pitch follows `contour` (list of Hz over time)
    with a formant sweeping from `brightness[0]` to `brightness[1]` Hz."""
    n = int(seconds * RATE)
    out, phase = [], 0.0
    for i in range(n):
        p = i / n
        pos = p * (len(contour) - 1)
        k = min(int(pos), len(contour) - 2)
        f = contour[k] + (contour[k + 1] - contour[k]) * (pos - k)
        f *= 1 + vibrato * math.sin(2 * math.pi * 6 * i / RATE)
        phase += f / RATE
        formant = brightness[0] + (brightness[1] - brightness[0]) * min(1.0, p * 3)
        s = 0.0
        for h in range(1, 9):
            weight = 1 / (1 + ((h * f - formant) / 500) ** 2)
            s += weight * math.sin(2 * math.pi * h * phase) / h ** 0.6
        env = min(1.0, p / 0.08) * min(1.0, (1 - p) / 0.25)
        out.append(s * env * gain)
    return out


def sfx_feed():
    """Bright two-note "pop" when the food bowl comes out."""
    t = Track(0.3, loop=False)
    t.add(0.0, square(freq("C6"), 0.09, duty=0.5))
    t.add(0.09, square(freq("G6"), 0.16, duty=0.5))
    t.write("sfx_feed.wav", peak=0.6)


def sfx_eat():
    """Three little munches."""
    t = Track(0.75, loop=False)
    for k in range(3):
        rnd = random.Random(k + 10)
        n = int(0.12 * RATE)
        crunch = []
        for i in range(n):
            tt = i / RATE
            noise = rnd.uniform(-1, 1) * 0.6
            tone = math.sin(2 * math.pi * (260 - 600 * tt) * tt)
            crunch.append((noise + tone) * math.exp(-tt * 28))
        t.add(k * 0.22, smooth(crunch, 4))
    t.write("sfx_eat.wav", peak=0.55)


def sfx_laugh():
    """Happy "nya-nya!" going up."""
    t = Track(0.6, loop=False)
    t.add(0.0, meow([650, 850, 800], 0.22))
    t.add(0.26, meow([750, 1000, 950], 0.28))
    t.write("sfx_laugh.wav", peak=0.6)


def sfx_cry():
    """Long, wobbly, falling "nyaaa..."."""
    t = Track(0.9, loop=False)
    t.add(0.0, meow([700, 820, 650, 480], 0.85, vibrato=0.025, brightness=(800, 1200)))
    t.write("sfx_cry.wav", peak=0.55)


def sfx_pet():
    """Soft "pon" when the cat is tapped."""
    t = Track(0.3, loop=False)
    t.add(0.0, bell(freq("A5"), 0.3))
    t.write("sfx_pet.mp3", peak=0.5)


def sfx_stretch():
    """A sleepy yawn-meow when waking up and stretching."""
    t = Track(0.9, loop=False)
    t.add(0.0, meow([420, 620, 560, 380], 0.85, brightness=(600, 1000)), 0.8)
    t.write("sfx_stretch.wav", peak=0.45)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    day_bgm()
    night_bgm()
    sfx_feed()
    sfx_eat()
    sfx_laugh()
    sfx_cry()
    sfx_pet()
    sfx_stretch()
