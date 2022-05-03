from setuptools import setup


setup(
    name="emap_runner",
    version="0.0.1",
    packages=["emap_runner"],
    author="Sarah Keating, Tom Young",
    url="https://github.com/inform-health-informatics/emap-setup",
    entry_points={"console_scripts": ["emap = emap_runner.runner:main"]},
    description="Setup, updating and docker orchestration of EMAP",
    platforms=["linux", "macos"]
)
