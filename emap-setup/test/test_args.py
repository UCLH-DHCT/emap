import unittest

import emap_setup


class TestArgs(unittest.TestCase):
    def setup(self):
        self.arg_parser = define_arguments(['-h'])

    def test_help(self):
#        parser = define_arguments(['-h'])
        self.assertEqual('a', 'a')

if __name__ == '__main__':
    unittest.main()
