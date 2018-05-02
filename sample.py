import os
import contextlib
import time
import requests
from subprocess import Popen

from logging import getLogger, StreamHandler, INFO
logger = getLogger(__name__)
handler = StreamHandler()
handler.setLevel(INFO)
logger.setLevel(INFO)
logger.addHandler(handler)
logger.propagate = False

API_URL = "http://localhost:6001"
TOOL_DIR = os.path.abspath(os.path.dirname(__file__))
TOOL_CMD = "./gradlew run"


@contextlib.contextmanager
def start():

    try:
        logger.info([TOOL_CMD, TOOL_DIR])

        with open(os.devnull, 'w') as devnull:
            proc = Popen(TOOL_CMD, shell=True, cwd=TOOL_DIR, stderr=devnull)
            logger.info("pdfbox server pid = %s" % proc.pid)
            assert proc.poll() is None
            time.sleep(5)   # とりあえず最初に多少待つ

            for i in range(10, 0, -1):
                try:
                    time.sleep(1)
                    requests.get("%s/ping" % API_URL)
                    break
                except requests.exceptions.ConnectionError as _:
                    logger.warn("... waiting server %d" % i)
            else:
                raise RuntimeError("pdfbox server start failed!!")

            yield API_URL

            assert proc.poll() is None
    finally:
        proc.terminate()
        proc.wait()
        assert proc.poll() is not None
        logger.info("pdfbox server terminated.")


def pdf_to_text(buf, api_url=API_URL):
    ret = requests.post(api_url, data=buf)
    assert ret.encoding == 'utf-8'
    return ret.text


if __name__ == '__main__':
    with start() as api_url:
        with open("sample.pdf", "rb") as fp:
            text = pdf_to_text(fp.read(), api_url)
            print(text)
